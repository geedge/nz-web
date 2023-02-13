//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.nis.modules.sys.shiro;

import org.crazycake.shiro.IRedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * MyRedisManager
 * 重写自 org.crazycake.shiro.RedisManager
 * 重写原因：shiro-redis 最高版本仅支持 Jedis 3.3.0，重写该类解决 jedis api 调用报错问题
 *
 * @author shizhendong
 * @date 2023-02-13
 */
public class MyRedisManager extends WorkAloneRedisManager implements IRedisManager {
    private static final String DEFAULT_HOST = "127.0.0.1:6379";
    private String host = "127.0.0.1:6379";
    private int timeout = 2000;
    private String password;
    private int database = 0;
    private JedisPool jedisPool;

    public MyRedisManager() {
    }

    private void init() {
        if (this.jedisPool == null) {
            Class var1 = org.crazycake.shiro.RedisManager.class;
            synchronized (org.crazycake.shiro.RedisManager.class) {
                if (this.jedisPool == null) {
                    String[] hostAndPort = this.host.split(":");
                    this.jedisPool = new JedisPool(this.getJedisPoolConfig(), hostAndPort[0], Integer.parseInt(hostAndPort[1]), this.timeout, this.password, this.database);
                }
            }
        }

    }

    @Override
    protected Jedis getJedis() {
        if (this.jedisPool == null) {
            this.init();
        }

        return this.jedisPool.getResource();
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return this.database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public JedisPool getJedisPool() {
        return this.jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }
}
