package org.noahsark.mqtt.broker.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.noahsark.mqtt.broker.common.redis.cmd.CmdEnum;
import org.noahsark.mqtt.broker.common.redis.cmd.OperationEnum;
import org.noahsark.mqtt.broker.common.redis.cmd.RedisCmd;
import org.noahsark.mqtt.broker.common.redis.cmd.Triple;
import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import redis.clients.jedis.JedisPool;

import java.util.UUID;

/**
 * Redis 测试
 *
 * @author zhangxt
 * @date 2021/11/4
 */
public class RedisRunnerTest {

    private static final String HOST = "192.168.7.115";

    private static final int PORT = 6379;

    private JedisPool jedisPool;

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    @Before
    public void before() {
        this.jedisPool = new JedisPool(buildPoolConfig(), HOST, PORT);

        this.cmdRunner = new RedisCmdRunner(this.jedisPool);

        this.scriptRunner = new RedisScriptRunner(this.jedisPool);
    }

    @After
    public void after() {
        this.jedisPool.close();

        this.jedisPool = null;
    }

    @Test
    public void redisCmdRunnerStringKeyTest() {

        RedisCmd cmd = new RedisCmd();
        cmd.setOperationType(OperationEnum.ADD);
        cmd.setType(CmdEnum.STRING);

        Triple key = new Triple();
        key.setKey("app");
        key.setValue("alligator".getBytes());

        cmd.setKey(key);

        cmdRunner.exeCmd(cmd);
    }

    @Test
    public void redisTryLockTest() {
        String key = "lock:order:123456";
        String uuid = UUID.randomUUID().toString();
        int expires = 10;

        System.out.println("uuid:" + uuid);

        System.out.println("first trylock:" + scriptRunner.tryLock(key, uuid, expires));

        System.out.println("release lock:");
        scriptRunner.releaseLock(key, uuid);

        System.out.println("second trylock:" + scriptRunner.tryLock(key, uuid, expires));
    }

    @Test
    public void incrTest() {
        String key = "seq:a:b:c";

        Long seq = cmdRunner.incr(key);

        System.out.println("seq = " + seq);
    }

    public GenericObjectPoolConfig buildPoolConfig() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

        poolConfig.setMaxTotal(GenericObjectPoolConfig.DEFAULT_MAX_TOTAL * 5);
        poolConfig.setMaxIdle(GenericObjectPoolConfig.DEFAULT_MAX_IDLE * 3);
        poolConfig.setMinIdle(GenericObjectPoolConfig.DEFAULT_MIN_IDLE);

        poolConfig.setJmxEnabled(true);
        poolConfig.setMaxWaitMillis(3000);

        return poolConfig;
    }


}
