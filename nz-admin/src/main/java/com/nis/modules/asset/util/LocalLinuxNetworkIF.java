/*
 * Copyright 2020-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package com.nis.modules.asset.util;


import cn.hutool.core.io.IoUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nis.common.utils.Tool;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ParseUtil;
import oshi.util.Util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * LinuxNetworks class.
 */
@ThreadSafe
@Data
public final class LocalLinuxNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(LocalLinuxNetworkIF.class);

    private String name;
    private int ifType;
    private int ifIndex;
    private long mtu;
    private long speed;
    private String address;
    private String ifAlias = "";
    private String ifOperStatus = "unknown";

    private static Session session;

    public LocalLinuxNetworkIF(Session session, String ifName) {
        this.session = session;
        this.name = this.queryIfModelFromSysfs(ifName);
        this.updateAttributes();
    }

    private String queryIfModelFromSysfs(String name) {
        Map<String, String> uevent = this.getKeyValueMapFromFile("/sys/class/net/" + name + "/uevent", "=");
        String devVendor = uevent.get("ID_VENDOR_FROM_DATABASE");
        String devModel = uevent.get("ID_MODEL_FROM_DATABASE");
        if (!Util.isBlank(devModel)) {
            if (!Util.isBlank(devVendor)) {
                return devVendor + " " + devModel;
            }
            return devModel;
        }
        return name;
    }

    public boolean updateAttributes() {
        String fileName = String.format(Locale.ROOT, "/sys/class/net/%s/statistics", getName());
        String ifDir = JschUtil.exec(session, "stat -c %F " + fileName, Charset.forName("UTF-8"));
        if (!Tool.StrUtil.equalsAnyIgnoreCase("directory", Tool.StrUtil.trim(ifDir))) {
            return false;
        }

        String ifTypePath = String.format(Locale.ROOT, "/sys/class/net/%s/type", getName());
        String ifIndexPath = String.format(Locale.ROOT, "/sys/class/net/%s/ifindex", getName());
        String mtuPath = String.format(Locale.ROOT, "/sys/class/net/%s/mtu", getName());
        String addressPath = String.format(Locale.ROOT, "/sys/class/net/%s/address", getName());

        String ifSpeed = String.format(Locale.ROOT, "/sys/class/net/%s/speed", getName());
        String ifAliasPath = String.format(Locale.ROOT, "/sys/class/net/%s/ifalias", getName());
        String ifOperStatusPath = String.format(Locale.ROOT, "/sys/class/net/%s/operstate", getName());

        this.ifType = this.getIntFromFile(ifTypePath);
        this.ifIndex = this.getIntFromFile(ifIndexPath);
        this.mtu = this.getUnsignedLongFromFile(mtuPath);
        long speedMiB = this.getUnsignedLongFromFile(ifSpeed);
        // speed may be -1 from file.
        this.speed = speedMiB < 0 ? 0 : speedMiB << 20;
        this.address = this.getStringFromFile(addressPath);
        this.ifAlias = this.getStringFromFile(ifAliasPath);
        this.ifOperStatus = this.getStringFromFile(ifOperStatusPath);
        return true;
    }

    public int getIntFromFile(String filename) {
        try {
            List<String> read = this.readRemoteFile(filename);
            if (!read.isEmpty()) {
                return Integer.parseInt((String) read.get(0));
            }
        } catch (NumberFormatException var2) {
        }
        return 0;
    }

    public long getUnsignedLongFromFile(String filename) {
        List<String> read = this.readRemoteFile(filename);
        if (!read.isEmpty()) {
            return ParseUtil.parseUnsignedLongOrDefault((String) read.get(0), 0L);
        } else {
            return 0L;
        }
    }

    public String getStringFromFile(String filename) {
        List<String> read = this.readRemoteFile(filename);
        if (!read.isEmpty()) {
            return (String) read.get(0);
        } else {
            return "";
        }
    }

    public Map<String, String> getKeyValueMapFromFile(String filename, String separator) {
        Map<String, String> map = new HashMap();

        List<String> lines = this.readRemoteFile(filename);
        Iterator var4 = lines.iterator();

        while (var4.hasNext()) {
            String line = (String) var4.next();
            String[] parts = line.split(separator);
            if (parts.length == 2) {
                map.put(parts[0], parts[1].trim());
            }
        }
        return map;
    }

    public List<String> readRemoteFile(String filename) {
        List<String> list = Tool.ListUtil.list(true);
        try {
            ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
            channel.setCommand("cat " + filename);
            channel.connect();

            IoUtil.readLines(channel.getInputStream(), Charset.forName("UTF-8"), list);
            JschUtil.close(channel);
        } catch (JSchException | IOException e) {
        }
        return list;
    }

    enum IfOperStatus {
        /**
         * Up and operational. Ready to pass packets.
         */
        UP(1),
        /**
         * Down and not operational. Not ready to pass packets.
         */
        DOWN(2),
        /**
         * In some test mode.
         */
        TESTING(3),
        /**
         * The interface status is unknown.
         */
        UNKNOWN(4),
        /**
         * The interface is not up, but is in a pending state, waiting for some external event.
         */
        DORMANT(5),
        /**
         * Some component is missing
         */
        NOT_PRESENT(6),
        /**
         * Down due to state of lower-layer interface(s).
         */
        LOWER_LAYER_DOWN(7);

        private final int value;

        IfOperStatus(int value) {
            this.value = value;
        }

        /**
         * @return the integer value specified in RFC 2863 for this operational status.
         */
        public int getValue() {
            return this.value;
        }
    }
}
