package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.repository.InflightMessageRepository;

import java.util.HashSet;
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
        cmdRunner.updateMessage(clientId, message, isSending);
    }

    @Override
    public void removeMessage(String clientId, int packetId, boolean isSending) {
        String key;
        if (isSending) {
            key = String.format(RedisConstant.SESSION_INFLIGHT_KEY_FORMAT, clientId);
        } else {
            key = String.format(RedisConstant.SESSION_RECEIVE_KEY_FORMAT, clientId);
        }

        cmdRunner.hdel(key, Integer.toString(packetId));
    }

    @Override
    public PublishInnerMessage getMessage(String clientId, int packetId, boolean isSending) {
        return cmdRunner.getMessage(clientId, packetId, isSending);
    }

    @Override
    public List<PublishInnerMessage> getAllMessages(String clientId, boolean isSending) {
        return cmdRunner.getAllMessage(clientId, isSending);
    }

    @Override
    public boolean contain(String clientId, int packetId, boolean isSending) {
        String key;
        if (isSending) {
            key = String.format(RedisConstant.SESSION_INFLIGHT_KEY_FORMAT, clientId);
        } else {
            key = String.format(RedisConstant.SESSION_RECEIVE_KEY_FORMAT, clientId);
        }
        return cmdRunner.isExistInHash(key, Integer.toString(packetId));
    }

    @Override
    public void addPubRel(String clientId, int packetId) {
        String key = String.format(RedisConstant.SESSION_PUBREL_KEY_FORMAT, clientId);

        cmdRunner.addElementFromSet(key, Integer.toString(packetId));
    }

    @Override
    public void removePubRel(String clientId, int packetId) {
        String key = String.format(RedisConstant.SESSION_PUBREL_KEY_FORMAT, clientId);

        cmdRunner.delElementFromSet(key, Integer.toString(packetId));

    }

    @Override
    public Set<Integer> getAllPubRel(String clientId) {

        String key = String.format(RedisConstant.SESSION_PUBREL_KEY_FORMAT, clientId);
        List<String> list = cmdRunner.getElementsFromSet(key);

        Set<Integer> set = new HashSet<>();

        list.forEach(value -> {
            set.add(Integer.parseInt(value));
        });

        return set;
    }

    @Override
    public void clean(String clientId) {
        String key;
        key = String.format(RedisConstant.SESSION_INFLIGHT_KEY_FORMAT, clientId);
        cmdRunner.del(key);

        key = String.format(RedisConstant.SESSION_RECEIVE_KEY_FORMAT, clientId);
        cmdRunner.del(key);

        key = String.format(RedisConstant.SESSION_PUBREL_KEY_FORMAT, clientId);
        cmdRunner.del(key);
    }
}
