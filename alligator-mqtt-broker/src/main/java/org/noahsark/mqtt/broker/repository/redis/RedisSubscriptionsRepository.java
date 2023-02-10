package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.clusters.serializer.ProtostuffUtils;
import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Redis 版 Subscription Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 16:07
 **/
public class RedisSubscriptionsRepository implements SubscriptionsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RedisSubscriptionsRepository.class);

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    public RedisSubscriptionsRepository(RedisCmdRunner cmdRunner, RedisScriptRunner scriptRunner) {
        this.cmdRunner = cmdRunner;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public List<StoredSubscription> getAllSubscriptions(String clientId) {
        return cmdRunner.getAllSubscriptions(clientId);
    }

    @Override
    public void addSubscription(String clientId, StoredSubscription subscription) {
        String key = String.format(RedisConstant.SESSION_SUBSCRIPTION_FORMAT, clientId);

        cmdRunner.hset(key, subscription.getTopicFilter(), ProtostuffUtils.serialize(subscription));
    }

    @Override
    public void removeSubscription(String clientId, String topic) {
        String key = String.format(RedisConstant.SESSION_SUBSCRIPTION_FORMAT, clientId);

        cmdRunner.hdel(key, topic);

        LOG.info("Remove clientId subscription,clientId:{},topic:{}", clientId, topic);
    }

    @Override
    public void clean(String clientId) {
        String key = String.format(RedisConstant.SESSION_SUBSCRIPTION_FORMAT, clientId);

        cmdRunner.del(key);

        LOG.info("Remove clientId subscriptions:{}", key);
    }
}
