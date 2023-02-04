package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredSubscription;

import java.util.List;

/**
 * Redis 版 Subscription Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 16:07
 **/
public class RedisSubscriptionsRepository implements SubscriptionsRepository {

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    public RedisSubscriptionsRepository(RedisCmdRunner cmdRunner, RedisScriptRunner scriptRunner) {
        this.cmdRunner = cmdRunner;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public List<StoredSubscription> getAllSubscriptions(String clientId) {
        return null;
    }

    @Override
    public void addSubscription(String clientId, StoredSubscription subscription) {

    }

    @Override
    public void removeSubscription(String clientId, String topic) {

    }
}
