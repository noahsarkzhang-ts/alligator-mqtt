package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.repository.OffsetRepository;

import java.util.Map;

/**
 * Redis 版 Offset Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 16:03
 **/
public class RedisOffsetRepository implements OffsetRepository {

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    public RedisOffsetRepository(RedisCmdRunner cmdRunner, RedisScriptRunner scriptRunner) {
        this.cmdRunner = cmdRunner;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public void addTopicOffset(String clientId, String topic, long offset) {

    }

    @Override
    public void updateTopicOffset(String clientId, String topic, long offset) {

    }

    @Override
    public int getTopicOffset(String clientId, String topic) {
        return 0;
    }

    @Override
    public Map<String, Integer> getAllTopicOffsets(String clientId) {
        return null;
    }
}
