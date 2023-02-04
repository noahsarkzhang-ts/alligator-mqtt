package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;

import java.util.List;
import java.util.Set;

/**
 * 发送中或接收中的消息 Repository 类
 *
 * @author zhangxt
 * @date 2023/01/30 10:06
 **/
public interface InflightMessageRepository {

    /**
     * 添加正在发送中或接收中的 Publish 消息
     *
     * @param clientId  客户端id
     * @param message   Publish 消息
     * @param isSending 发送中/接收中
     */
    void addMessage(String clientId, PublishInnerMessage message, boolean isSending);

    /**
     * 移除正在发送中或接收中的 Publish 消息
     *
     * @param clientId  客户端id
     * @param packetId  消息id
     * @param isSending 发送中/接收中
     */
    void removeMessage(String clientId, int packetId, boolean isSending);

    /**
     * 获取正在发送中或接收中的 Publish 消息
     *
     * @param clientId  客户端id
     * @param packetId  消息id
     * @param isSending 发送中/接收中
     */
    PublishInnerMessage getMessage(String clientId, int packetId, boolean isSending);

    /**
     * 获取所有正在发送中或接收中的 Publish 消息
     *
     * @param clientId  客户端id
     * @param isSending 发送中/接收中
     */
    List<PublishInnerMessage> getAllMessages(String clientId, boolean isSending);

    /**
     * 判断是否包含该 packetId
     *
     * @param clientId  客户端id
     * @param packetId  消息id
     * @param isSending 发送中/接收中
     * @return
     */
    boolean contain(String clientId, int packetId, boolean isSending);

    /**
     * 添加发送中的 PubRel消息，只需要存储 packetId 即可
     *
     * @param clientId 客户端id
     * @param packetId 消息id
     */
    void addPubRel(String clientId, int packetId);

    /**
     * 移除发送中的 PubRel消息
     *
     * @param clientId 客户端id
     * @param packetId 消息id
     */
    void removePubRel(String clientId, int packetId);

    /**
     * 获取所有发送中的 PubRel消息
     *
     * @param clientId 客户端id
     */
    Set<Integer> getAllPubRel(String clientId);

    /**
     * 清空 inflight 数据
     * @param clientId 客户端id
     */
    void clean(String clientId);

}
