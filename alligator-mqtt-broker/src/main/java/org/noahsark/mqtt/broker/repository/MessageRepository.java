package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.protocol.entity.StoredMessage;

import java.util.List;

/**
 * QOS1,QOS2消息存储类
 *
 * @author zhangxt
 * @date 2022/11/25 10:06
 **/
public interface MessageRepository {

    void store(StoredMessage msg);

    StoredMessage getMessageById(String topic, Integer id);

    List<StoredMessage> queryMessages(String topic, Integer offset);
}
