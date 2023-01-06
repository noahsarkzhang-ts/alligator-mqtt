package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.protocol.entity.StoredMessage;

import java.util.List;

/**
 * Mysql 版本的 MessageRepository
 *
 * @author zhangxt
 * @date 2022/11/25 14:23
 **/
public class MysqlMessageRepository implements MessageRepository {
    @Override
    public void store(StoredMessage msg) {

    }

    @Override
    public StoredMessage getMessageById(String topic, Integer id) {
        return null;
    }

    @Override
    public List<StoredMessage> queryMessages(String topic, Integer offset) {
        return null;
    }
}
