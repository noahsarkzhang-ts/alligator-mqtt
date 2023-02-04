package org.noahsark.mqtt.broker.repository.factory;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.repository.ClientSessionRepository;
import org.noahsark.mqtt.broker.repository.InflightMessageRepository;
import org.noahsark.mqtt.broker.repository.OffsetGenerator;
import org.noahsark.mqtt.broker.repository.OffsetRepository;
import org.noahsark.mqtt.broker.repository.RetainedRepository;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.WillRepository;
import org.noahsark.mqtt.broker.repository.redis.RedisClientSessionRepository;
import org.noahsark.mqtt.broker.repository.redis.RedisInflightMessageRepository;
import org.noahsark.mqtt.broker.repository.redis.RedisOffsetGenerator;
import org.noahsark.mqtt.broker.repository.redis.RedisOffsetRepository;
import org.noahsark.mqtt.broker.repository.redis.RedisRetainedRepository;
import org.noahsark.mqtt.broker.repository.redis.RedisSubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.redis.RedisWillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis版本的缓存工厂类
 *
 * @author zhangxt
 * @date 2023/01/30 17:21
 **/
public class RedisCacheBeanFactory implements CacheBeanFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RedisCacheBeanFactory.class);

    /**
     * 缓存对象
     */
    protected Map<Class<?>, Object> beans = new HashMap<>();

    private GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

    private JedisPool jedisPool;

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    private String host;

    private int port;

    @Override
    public ClientSessionRepository clientSessionRepository() {
        return getBean(RedisClientSessionRepository.class);
    }

    @Override
    public InflightMessageRepository inflightMessageRepository() {
        return getBean(RedisInflightMessageRepository.class);
    }

    @Override
    public OffsetGenerator offsetGenerator() {

        return getBean(RedisOffsetGenerator.class);
    }

    @Override
    public OffsetRepository offsetRepository() {
        return getBean(RedisOffsetRepository.class);
    }

    @Override
    public RetainedRepository retainedRepository() {
        return getBean(RedisRetainedRepository.class);
    }

    @Override
    public SubscriptionsRepository subscriptionsRepository() {
        return getBean(RedisSubscriptionsRepository.class);
    }

    @Override
    public WillRepository willRepository() {
        return getBean(RedisWillRepository.class);
    }

    private <T> T getBean(Class<T> classz) {
        T bean = (T) beans.get(classz);

        if (bean == null) {

            Constructor constroctor;
            try {
                constroctor = classz.getConstructor(RedisCmdRunner.class, RedisScriptRunner.class);
                bean = (T) constroctor.newInstance(this.cmdRunner, this.scriptRunner);

                beans.put(classz, bean);

            } catch (Exception ex) {
                LOG.info("Catch an exception while load class.", ex);
            }

        }

        return bean;
    }

    @Override
    public void init() {
        this.jedisPool = new JedisPool(poolConfig, host, port);
        this.cmdRunner = new RedisCmdRunner(this.jedisPool);
        this.scriptRunner = new RedisScriptRunner(this.jedisPool);
    }

    @Override
    public void load(Configuration configuration) {
        // 从配置文件中加载配置 TODO

        poolConfig.setMaxTotal(GenericObjectPoolConfig.DEFAULT_MAX_TOTAL * 5);
        poolConfig.setMaxIdle(GenericObjectPoolConfig.DEFAULT_MAX_IDLE * 3);
        poolConfig.setMinIdle(GenericObjectPoolConfig.DEFAULT_MIN_IDLE);

        poolConfig.setJmxEnabled(true);
        poolConfig.setMaxWaitMillis(3000);

        // 从配置文件中读取
        host = "192.168.7.115";
        port = 6379;

        LOG.info("load:{}", RedisCacheBeanFactory.class.getSimpleName());
    }

    @Override
    public String alias() {
        return "redis";
    }
}
