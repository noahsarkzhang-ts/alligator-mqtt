package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.clusters.serializer.ProtobufSerializer;
import org.noahsark.mqtt.broker.clusters.serializer.ProtostuffUtils;
import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.protocol.entity.RetainedMessage;
import org.noahsark.mqtt.broker.repository.RetainedRepository;

import java.util.List;

/**
 * Redis版 RetainRepository 类
 *
 * @author zhangxt
 * @date 2023/01/31 16:05
 **/
public class RedisRetainedRepository implements RetainedRepository {

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    public RedisRetainedRepository(RedisCmdRunner cmdRunner, RedisScriptRunner scriptRunner) {
        this.cmdRunner = cmdRunner;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public void clean(String topic) {
        String key = String.format(RedisConstant.TOPIC_RETAIN_FORMAT, topic);

        cmdRunner.del(key);
    }

    @Override
    public void addRetainMessage(String topic, RetainedMessage msg) {
        String key = String.format(RedisConstant.TOPIC_RETAIN_FORMAT, topic);
        cmdRunner.rpush(key, ProtostuffUtils.serialize(msg));
    }

    @Override
    public List<RetainedMessage> getAllRetainMessage(String topic) {
        return cmdRunner.getAllRetainMessage(topic);
    }
}
