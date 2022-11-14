/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nis.modules.terminal.config;

import cn.hutool.log.Log;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nis.common.utils.Tool;
import lombok.Data;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Concurrent/Multiple Connections.
 * SFTPConnectionPool
 */
@Data
public class SFTPConnectionPool {

    private static final Log log = Log.get();

    /**
     * Maximum number of allowed live connections. This doesn't mean we cannot
     * have more live connections. It means that when we have more
     * live connections than this threshold, any unused connection will be
     *  closed.
     */
    private Session session;
    private Integer maxConnection;
    private Integer liveConnectionCount = 0;
    private HashMap<String, HashSet<ChannelSftp>> idleConnections = Tool.MapUtil.newHashMap();
    private HashMap<ChannelSftp, String> con2infoMap = Tool.MapUtil.newHashMap();

    public SFTPConnectionPool(Session session, int maxConnection) {
        this.session = session;
        this.maxConnection = maxConnection;
    }

    /**
     * getIdleChannel
     *
     * @param uuid
     * @return
     * @throws IOException
     */
    public ChannelSftp getIdleChannel(String uuid) throws IOException {
        try {
            ChannelSftp channel = getChannelFromPool(uuid);
            if (channel != null) {
                return channel;
            }

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            synchronized (this) {
                con2infoMap.put(channel, uuid);
                liveConnectionCount++;
            }
            return channel;
        } catch (JSchException e) {
            throw new IOException(stringifyException(e));
        }
    }

    private ChannelSftp getChannelFromPool(String uuid) throws IOException {
        // get connection from pool
        ChannelSftp channel = getFromPool(uuid);

        if (channel != null) {
            if (channel.isConnected()) {
                return channel;
            } else {
                channel = null;
                synchronized (this) {
                    --liveConnectionCount;
                    con2infoMap.remove(channel);
                }
            }
        }
        return null;
    }

    synchronized ChannelSftp getFromPool(String uuid) throws IOException {
        Set<ChannelSftp> cons = idleConnections.get(uuid);
        ChannelSftp channel;
        if (cons != null && cons.size() > 0) {
            Iterator<ChannelSftp> it = cons.iterator();
            if (it.hasNext()) {
                channel = it.next();
                idleConnections.remove(uuid);
                return channel;
            } else {
                throw new IOException("Connection pool error.");
            }
        }
        return null;
    }

    /**
     * Add the channel into pool.
     *
     * @param channel
     */
    synchronized void returnToPool(ChannelSftp channel) {
        String uuid = con2infoMap.get(channel);
        HashSet<ChannelSftp> cons = idleConnections.get(uuid);
        if (cons == null) {
            cons = new HashSet<ChannelSftp>();
        }
        cons.add(channel);
        idleConnections.put(uuid, cons);
    }

    /**
     * Shutdown the connection pool and close all open connections.
     */
    synchronized void shutdown() {
        if (this.con2infoMap == null) {
            return; // already shutdown in case it is called
        }
        log.info("Inside shutdown, con2infoMap size=" + con2infoMap.size());

        this.maxConnection = 0;
        Set<ChannelSftp> cons = con2infoMap.keySet();
        if (cons != null && cons.size() > 0) {
            // make a copy since we need to modify the underlying Map
            Set<ChannelSftp> copy = new HashSet<ChannelSftp>(cons);
            // Initiate disconnect from all outstanding connections
            for (ChannelSftp con : copy) {
                try {
                    disconnect(con);
                } catch (IOException ioe) {
                    String uuid = con2infoMap.get(con);
                    log.error(ioe, "Error encountered while closing connection to " + uuid);
                }
            }
        }
        // make sure no further connections can be returned.
        this.idleConnections = null;
        this.con2infoMap = null;
    }

    public void disconnect(ChannelSftp channel) throws IOException {
        if (channel != null) {
            // close connection if too many active connections
            boolean closeConnection = false;
            synchronized (this) {
                if (liveConnectionCount > maxConnection) {
                    --liveConnectionCount;
                    con2infoMap.remove(channel);
                    closeConnection = true;
                }
            }
            if (closeConnection) {
                if (channel.isConnected()) {
                    try {
                        Session session = channel.getSession();
                        channel.disconnect();
                        session.disconnect();
                    } catch (JSchException e) {
                        throw new IOException(stringifyException(e));
                    }
                }
            } else {
                returnToPool(channel);
            }
        }
    }

    /**
     * stringifyException
     *
     * @param e
     * @return
     */
    public static String stringifyException(Throwable e) {
        StringWriter stm = new StringWriter();
        PrintWriter wrt = new PrintWriter(stm);
        e.printStackTrace(wrt);
        wrt.close();
        return stm.toString();
    }
}
