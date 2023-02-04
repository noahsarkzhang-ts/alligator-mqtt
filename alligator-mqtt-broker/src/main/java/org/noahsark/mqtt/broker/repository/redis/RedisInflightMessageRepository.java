package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.repository.InflightMessageRepository;

import java.util.List;
import java.util.Set;

/**
 * Redis 版 Inflight Message Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 16:02
 **/
public class RedisInflightMessageRepository implements InflightMessageRepository {

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    public RedisInflightMessageRepository(RedisCmdRunner cmdRunner, RedisScriptRunner scriptRunner) {
        this.cmdRunner = cmdRunner;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public void addMessage(String clientId, PublishInnerMessage message, boolean isSending) {

    }

    @Override
    public void removeMessage(String clientId, int packetId, boolean isSending) {

    }

    @Override
    public PublishInnerMessage getMessage(String clientId, int packetId, boolean isSending) {
        return null;
    }

    @Override
    public List<PublishInnerMessage> getAllMessages(String clientId, boolean isSending) {
        return null;
    }

    @Override
    public boolean contain(String clientId, int packetId, boolean isSending) {
        return false;
    }

    @Override
    public void addPubRel(String clientId, int packetId) {

    }

    @Override
    public void removePubRel(String clientId, int packetId) {

    }

    @Override
    public Set<Integer> getAllPubRel(String clientId) {
        return null;
    }
}
