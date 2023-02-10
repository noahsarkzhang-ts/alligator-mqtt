package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.repository.OffsetRepository;

import java.math.BigDecimal;
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
        String key = String.format(RedisConstant.SESSION_TOPIC_OFFSET_FORMAT, clientId);

        cmdRunner.hset(key, topic, Long.toString(offset).getBytes());
    }

    @Override
    public void updateTopicOffset(String clientId, String topic, long offset) {
        String key = String.format(RedisConstant.SESSION_TOPIC_OFFSET_FORMAT, clientId);

        cmdRunner.hset(key, topic, Long.toString(offset).getBytes());
    }

    @Override
    public long getTopicOffset(String clientId, String topic) {

        String key = String.format(RedisConstant.SESSION_TOPIC_OFFSET_FORMAT, clientId);
        BigDecimal offset = cmdRunner.getHashNumValue(key, topic);

        return offset.longValue();
    }

    @Override
    public Map<String, Long> getAllTopicOffsets(String clientId) {
        return cmdRunner.getAllTopicOffsets(clientId);
    }
}
