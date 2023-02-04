package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.repository.entity.StoredMessage;

import java.util.List;

/**
 * QOS1,QOS2消息 Repository 类
 *
 * @author zhangxt
 * @date 2022/11/25 10:06
 **/
public interface MessageRepository {

    /**
     * Publish 消息入库
     * Offset: 全局的消息id
     *
     * @param msg Publish 消息
     */
    void addMessage(StoredMessage msg);

    /**
     * 根据 offset 获取消息id
     *
     * @param topic  消息主题
     * @param offset offset
     * @return Publish 消息
     */
    StoredMessage getMessage(String topic, long offset);

    /**
     * 获取指定 offset 之后的消息
     *
     * @param topic       消息主题
     * @param startOffset 起始的offset
     * @return 消息列表
     */
    List<StoredMessage> getAllMessage(String topic, long startOffset);
}
