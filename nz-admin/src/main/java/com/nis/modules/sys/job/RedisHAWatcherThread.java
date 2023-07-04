package com.nis.modules.sys.job;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.SpringContextUtils;
import com.nis.common.utils.Tool;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * RedisHAWatcherThread
 * 前置条件: redis.mode=dms, redis.ur1.size=2
 * 状态变更: 以连续的状态判断 状态变化,比如:连续两次 UP 即为 UP,连续三次 DOWN 即为 DOWN
 * 线程逻辑: 检查 redis 服务状态,根据状态变化切换 master/slave 状态,同时更改 redisTemplate 连接信息
 *
 * @author shizhendong
 * @date 2023-07-03
 */
@Data
@Component
public class RedisHAWatcherThread implements Runnable {

    private static final Log log = Log.get();

    /**
     * 可选值: standalone,sentinel,dms  默认: standalone
     */
    @Value("${redis.mode:standalone}")
    private String redisMode;

    /**
     * 格式： host:port
     * 多个用逗号分割
     */
    @Value("${redis.ur1}")
    private String redisUrl;
    @Value("${redis.pin}")
    private String redisPin;
    @Value("${redis.database:0}")
    private int redisDatabase;

    /**
     * 检查周期, 默认: 10 单位：秒
     */
    @Value("${nezha.redisHAWatcherThread.interval:10}")
    private Integer jobInterval;

    /**
     * 超时时间，默认：5000  单位：毫秒
     */
    @Value("${nezha.redisHAWatcherThread.timeout:5000}")
    private Integer timeout;

    /**
     * 连续 2次 检查状态成功后，将服务状态从 DOWN 切换为 UP
     */
    @Value("${nezha.redisHAWatcherThread.rise:2}")
    private Integer rise;

    /**
     * 连续 3次 检查状态失败后，将服务状态从 UP 切换为 DOWN
     */
    @Value("${nezha.redisHAWatcherThread.fall:3}")
    private Integer fall;


    private static Queue<Integer> MASTER_STATE_QUEUE = new LinkedList<>();
    private static Queue<Integer> SLAVE_STATE_QUEUE = new LinkedList<>();

    @Override
    public void run() {
        Thread.currentThread().setName("RedisHAWatcherThread");

        if (!StrUtil.equalsIgnoreCase("dms", this.getRedisMode())) {
            log.warn("[RedisHAWatcherThread] [Task skipped] [redis.mode is not set to 'dms']");
            return;
        }

        List<String> redisUrlList = StrUtil.split(this.getRedisUrl(), ",");
        redisUrlList.sort(Comparator.naturalOrder());
        log.info("[RedisHAWatcherThread] [redisUrl: {}] [size: {}]", this.getRedisUrl(), redisUrlList.size());

        if (ObjectUtil.notEqual(2, redisUrlList.size())) {
            log.warn("[RedisHAWatcherThread] [Task skipped] [Invalid redis.url configuration: there are and only 2 addresses]");
            return;
        }

        HostAndPort masterAddress = HostAndPort.parseString(redisUrlList.get(0));
        HostAndPort slaveAddress = HostAndPort.parseString(redisUrlList.get(1));
        log.info("[RedisHAWatcherThread] [expected role] [master: {}] [slave: {}]", masterAddress.toString(), slaveAddress.toString());

        // init Master Slave State
        this.initMasterSlaveState(masterAddress, slaveAddress);

        while (true) {
            try {
                this.watcher(masterAddress, slaveAddress);
                // 十秒一次
                Tool.ThreadUtil.sleep(jobInterval, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error(e, "[RedisHAWatcherThread] [error]");
            }
        }
    }

    private void watcher(HostAndPort masterAddress, HostAndPort slaveAddress) {
        // 检查并记录 redis 服务状态，将结果写到队列中
        this.recordRedisStatusOfAvailable(masterAddress, MASTER_STATE_QUEUE);
        this.recordRedisStatusOfAvailable(slaveAddress, SLAVE_STATE_QUEUE);

        if (MASTER_STATE_QUEUE.size() < this.getRise() + this.getFall()) {
            log.warn("[RedisHAWatcherThread] [Task skipped] [redis service state not confirmed] [state_queue size: {}]", MASTER_STATE_QUEUE.size());
            return;
        }

        // 判断状态变化
        // master  DOWN -> UP
        boolean masterStateDownToUp = this.isStateDownToUp(MASTER_STATE_QUEUE, this.getRise(), this.getFall());
        if (masterStateDownToUp) {
            log.info("[RedisHAWatcherThread] [master status change] [DOWN -> UP]");
            // 切换为主
            this.switchToMaster(masterAddress, slaveAddress, true);

            // 切换为从
            this.switchToSlave(slaveAddress, masterAddress);
        }

        // master UP -> DOWN
        boolean masterStateUpToDown = this.isStateUpToDown(MASTER_STATE_QUEUE, this.getRise(), this.getFall());
        if (masterStateUpToDown) {
            log.info("[RedisHAWatcherThread] [master status change] [UP -> DOWN]");

            // 切换为主
            this.switchToMaster(slaveAddress, masterAddress, false);
        }
    }

    /**
     * init Master Slave State
     *
     * @param masterAddress
     * @param slaveAddress
     */
    private void initMasterSlaveState(HostAndPort masterAddress, HostAndPort slaveAddress) {
        log.info("[initMasterSlaveState] [begin] [master: {}] [slave: {}]", masterAddress, slaveAddress);
        // master
        try (Jedis jedis = new Jedis(masterAddress.getHost(), masterAddress.getPort(), this.getTimeout())) {
            if (StrUtil.isNotEmpty(this.getRedisPin()))
                jedis.auth(this.getRedisPin());
            String slaveofNoOne = jedis.slaveofNoOne();
            log.info("[initMasterSlaveState] [master status completed] [status: {}]", slaveofNoOne);
        } catch (Exception e) {
            log.error(e, "[initMasterSlaveState] [master error]");
        }

        // slave
        try (Jedis jedis = new Jedis(slaveAddress.getHost(), slaveAddress.getPort(), this.getTimeout())) {
            if (StrUtil.isNotEmpty(this.getRedisPin()))
                jedis.auth(this.getRedisPin());
            String slaveof = jedis.slaveof(masterAddress.getHost(), masterAddress.getPort());
            log.info("[initMasterSlaveState] [slave status completed] [status: {}]", slaveof);
        } catch (Exception e) {
            log.error(e, "[initMasterSlaveState] [slave error]");
        }
        log.info("[initMasterSlaveState] [finshed]");
    }

    /**
     * switch To Master
     *
     * @param toMasterAddress
     * @param replSourceAddress
     * @param isRepl
     */
    private void switchToMaster(HostAndPort toMasterAddress, HostAndPort replSourceAddress, Boolean isRepl) {
        log.info("[switchToMaster] [toMasterAddress: {}] [replSourceAddress: {}] [isRepl: {}]", toMasterAddress, replSourceAddress, isRepl);
        String host = toMasterAddress.getHost();
        int port = toMasterAddress.getPort();
        try (Jedis jedis = new Jedis(host, port, this.getTimeout())) {
            if (StrUtil.isNotEmpty(this.getRedisPin()))
                jedis.auth(this.getRedisPin());

            // 是否同步数据
            if (isRepl) {
                String slaveof = jedis.slaveof(replSourceAddress.getHost(), replSourceAddress.getPort());
                log.info("[switchToMaster] [sync data] [status: {}]", slaveof);
                // 等待同步
                Tool.ThreadUtil.sleep(10, TimeUnit.SECONDS);
            }

            // 切换角色为 master
            String slaveofNoOne = jedis.slaveofNoOne();

            // update Redis DataSource
            this.updateRedisDataSource(host, port);
            log.info("[switchToMaster] [success] [master: {}] [status: {}]", toMasterAddress, slaveofNoOne);
        } catch (Exception e) {
            log.error(e, "[switchToMaster] [error]");
        }
    }

    /**
     * switch To Slave
     *
     * @param toSlaveAddress
     * @param replSourceAddress
     */
    private void switchToSlave(HostAndPort toSlaveAddress, HostAndPort replSourceAddress) {
        log.info("[switchToSlave] [toSlaveAddress: {}] [replSourceAddress: {}]", toSlaveAddress, replSourceAddress);
        String host = toSlaveAddress.getHost();
        int port = toSlaveAddress.getPort();
        try (Jedis jedis = new Jedis(host, port, this.getTimeout())) {
            if (StrUtil.isNotEmpty(this.getRedisPin()))
                jedis.auth(this.getRedisPin());

            // 切换角色为 slave
            String slaveof = jedis.slaveof(replSourceAddress.getHost(), replSourceAddress.getPort());
            log.info("[switchToSlave] [success] [slave: {}] [status: {}]", toSlaveAddress, slaveof);
        } catch (Exception e) {
            log.error(e, "[switchToSlave] [error]");
        }
    }

    /**
     * update Redis DataSource
     *
     * @param host
     * @param port
     */
    private void updateRedisDataSource(String host, Integer port) {
        log.info("[updateRedisDataSource] [begin] [host: {}] [port: {}]", host, port);
        try {
            // connection factory
            LettuceConnectionFactory connectionFactory = (LettuceConnectionFactory) SpringContextUtils.getBean("redisConnectionFactory", RedisConnectionFactory.class);
            // release connection
            LettuceConnection connection = (LettuceConnection) connectionFactory.getConnection();
            RedisConnectionUtils.releaseConnection(connection, connectionFactory);

            // update connection info
            RedisStandaloneConfiguration standaloneConfiguration = connectionFactory.getStandaloneConfiguration();
            standaloneConfiguration.setHostName(host);
            standaloneConfiguration.setPort(port);

            standaloneConfiguration.setDatabase(this.getRedisDatabase());
            if (Tool.StrUtil.isNotBlank(this.getRedisPin())) {
                standaloneConfiguration.setPassword(this.getRedisPin());
            }

            // after Properties Set
            connectionFactory.afterPropertiesSet();
            // reset Connection
            connectionFactory.resetConnection();

            // redisTemplate
            RedisTemplate redisTemplate = SpringContextUtils.getBean("redisTemplate", RedisTemplate.class);
            redisTemplate.setConnectionFactory(connectionFactory);
            redisTemplate.afterPropertiesSet();

            // ping
            redisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    String ping = connection.ping();
                    if (Tool.StrUtil.isBlank(ping)) {
                        log.error("[updateRedisTemplateConfig] [redis connetion error] [result: {}]", ping);
                    }
                    return null;
                }
            });

            // stringRedisTemplate
            StringRedisTemplate stringRedisTemplate = SpringContextUtils.getBean("stringRedisTemplate", StringRedisTemplate.class);
            stringRedisTemplate.setConnectionFactory(connectionFactory);
            stringRedisTemplate.afterPropertiesSet();
        } catch (Exception e) {
            log.error(e, "[updateRedisDataSource] [error]");
        }
        log.info("[updateRedisDataSource] [finshed]");
    }

    /**
     * 判断状态是否由 DOWN -> UP
     *
     * @param queue
     * @param rise
     * @param fall
     * @return
     */
    private boolean isStateDownToUp(Queue<Integer> queue, Integer rise, Integer fall) {
        Long downCount = queue.stream()
                .limit(fall)
                .filter(state -> ObjectUtil.equal(0, state))
                .count();

        Long upCount = queue.stream()
                .skip(fall)
                .filter(state -> ObjectUtil.equal(1, state))
                .count();

        // 队列前面都是 UP 后面都是 DOWN, 证明状态由 DOWN -> UP
        // 例: 11000
        boolean isStateDownToUp = Tool.BooleanUtil.and(
                ObjectUtil.equal(fall, downCount.intValue()),
                ObjectUtil.equal(rise, upCount.intValue())
        );
        return isStateDownToUp;
    }

    /**
     * 判断状态是否由 UP -> DOWN
     *
     * @param queue
     * @param rise
     * @param fall
     * @return
     */
    private boolean isStateUpToDown(Queue<Integer> queue, Integer rise, Integer fall) {
        Long upCount = queue.stream()
                .limit(rise)
                .filter(state -> ObjectUtil.equal(1, state))
                .count();

        Long downCount = queue.stream()
                .skip(rise)
                .filter(state -> ObjectUtil.equal(0, state))
                .count();

        // 队列前面都是 DOWN 后面都是 UP, 证明状态由 UP -> DOWN
        // 例: 00011
        boolean isStateUpToDown = Tool.BooleanUtil.and(
                ObjectUtil.equal(rise, upCount.intValue()),
                ObjectUtil.equal(fall, downCount.intValue())
        );
        return isStateUpToDown;
    }

    /**
     * record Redis Status Of Available
     *
     * @param address
     * @param queue
     * @return
     */
    private void recordRedisStatusOfAvailable(HostAndPort address, Queue<Integer> queue) {
        String host = address.getHost();
        int port = address.getPort();
        Integer state = 0;
        try (Jedis jedis = new Jedis(host, port, this.getTimeout())) {
            if (StrUtil.isNotEmpty(this.getRedisPin()))
                jedis.auth(this.getRedisPin());
            String pong = jedis.ping();
            if (StrUtil.equalsIgnoreCase("pong", pong)) {
                state = 1;
            }
        } catch (Exception e) {
        }

        // record state
        int maxSize = this.getRise() + this.getFall();
        if (queue.size() >= maxSize) {
            // 舍弃队首元素
            queue.poll();
        }
        // 入队新元素
        queue.offer(state);
        log.info("[recordRedisStatusOfAvailable] [address: {}] [history state: {}]", address, JSONUtil.toJsonStr(queue));
    }

}
