//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.nis.modules.sys.shiro;

import org.crazycake.shiro.IRedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * WorkAloneRedisManager
 * 重写自 org.crazycake.shiro.common.WorkAloneRedisManager
 * 重写原因：shiro-redis 最高版本仅支持 Jedis 3.3.0，重写该类解决 jedis api 调用报错问题
 *
 * @author shizhendong
 * @date 2023-02-13
 */
public abstract class WorkAloneRedisManager implements IRedisManager {
    protected static final int DEFAULT_COUNT = 100;
    private int count = 100;
    private JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

    public WorkAloneRedisManager() {
    }

    protected abstract Jedis getJedis();

    @Override
    public byte[] get(byte[] key) {
        if (key == null) {
            return null;
        } else {
            Jedis jedis = this.getJedis();

            byte[] value;
            try {
                value = jedis.get(key);
            } finally {
                jedis.close();
            }

            return value;
        }
    }

    @Override
    public byte[] set(byte[] key, byte[] value, int expireTime) {
        if (key == null) {
            return null;
        } else {
            Jedis jedis = this.getJedis();

            try {
                jedis.set(key, value);
                if (expireTime > 0) {
                    jedis.expire(key, (long) expireTime);
                }
            } finally {
                jedis.close();
            }

            return value;
        }
    }

    @Override
    public void del(byte[] key) {
        if (key != null) {
            Jedis jedis = this.getJedis();

            try {
                jedis.del(key);
            } finally {
                jedis.close();
            }

        }
    }

    @Override
    public Long dbSize(byte[] pattern) {
        long dbSize = 0L;
        Jedis jedis = this.getJedis();

        try {
            ScanParams params = new ScanParams();
            params.count(this.count);
            params.match(pattern);
            byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;

            ScanResult scanResult;
            do {
                scanResult = jedis.scan(cursor, params);
                List<byte[]> results = scanResult.getResult();

                for (Iterator var9 = results.iterator(); var9.hasNext(); ++dbSize) {
                    byte[] result = (byte[]) var9.next();
                }

                cursor = scanResult.getCursorAsBytes();
            } while (scanResult.getCursor().compareTo(ScanParams.SCAN_POINTER_START) > 0);
        } finally {
            jedis.close();
        }

        return dbSize;
    }

    @Override
    public Set<byte[]> keys(byte[] pattern) {
        Set<byte[]> keys = new HashSet();
        Jedis jedis = this.getJedis();

        try {
            ScanParams params = new ScanParams();
            params.count(this.count);
            params.match(pattern);
            byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;

            ScanResult scanResult;
            do {
                scanResult = jedis.scan(cursor, params);
                keys.addAll(scanResult.getResult());
                cursor = scanResult.getCursorAsBytes();
            } while (scanResult.getCursor().compareTo(ScanParams.SCAN_POINTER_START) > 0);
        } finally {
            jedis.close();
        }

        return keys;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public JedisPoolConfig getJedisPoolConfig() {
        return this.jedisPoolConfig;
    }

    public void setJedisPoolConfig(JedisPoolConfig jedisPoolConfig) {
        this.jedisPoolConfig = jedisPoolConfig;
    }
}
