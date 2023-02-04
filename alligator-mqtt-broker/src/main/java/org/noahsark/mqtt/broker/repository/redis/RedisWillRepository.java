package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.protocol.entity.Will;
import org.noahsark.mqtt.broker.repository.WillRepository;

/**
 * Redis 版 Will Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 16:09
 **/
public class RedisWillRepository implements WillRepository {

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    public RedisWillRepository(RedisCmdRunner cmdRunner, RedisScriptRunner scriptRunner) {
        this.cmdRunner = cmdRunner;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public Will getWill(String clientId) {
        return null;
    }

    @Override
    public void addWill(String clientId, Will will) {

    }

    @Override
    public void updateWill(String clientId, Will will) {

    }

    @Override
    public void removeWill(String clientId) {

    }
}
