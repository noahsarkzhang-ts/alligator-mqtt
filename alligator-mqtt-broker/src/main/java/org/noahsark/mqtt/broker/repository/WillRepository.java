package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.protocol.entity.Will;

/**
 * Will Repository 类
 *
 * @author zhangxt
 * @date 2023/01/30 10:04
 **/
public interface WillRepository {

    /**
     * 获取指定 clientId 的Will消息
     *
     * @param clientId 客户端id
     * @return Will消息
     */
    Will getWill(String clientId);

    /**
     * 添加指定 clientId 的Will消息
     *
     * @param clientId 客户端id
     * @param will     Will 消息
     */
    void addWill(String clientId, Will will);

    /**
     * 更新指定 clientId 的Will消息
     *
     * @param clientId 客户端id
     * @param will     Will 消息
     */
    void updateWill(String clientId, Will will);

    /**
     * 移除指定客户端的Will消息
     *
     * @param clientId 客户端id
     */
    void removeWill(String clientId);

}
