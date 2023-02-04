package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.repository.entity.StoredSession;

/**
 * 客户端会话 Repository 类
 *
 * @author zhangxt
 * @date 2023/01/29 15:24
 **/
public interface ClientSessionRepository {

    /**
     * 获取指定 clientId 的会话信息
     *
     * @param clientId 客户端id
     * @return 客户端会话
     */
    StoredSession getSession(String clientId);

    /**
     * 添加 clientId 的会话消息
     *
     * @param clientId 客户端id
     * @param session  会话信息
     */
    void addSession(String clientId, StoredSession session);

    /**
     * 更新 clientId 的会话消息
     *
     * @param clientId 客户端id
     * @param session  会话信息
     */
    void updateSession(String clientId, StoredSession session);

    /**
     * 移除 clientId 的会话信息
     *
     * @param clientId 客户端id
     */
    void removeSession(String clientId);

    /**
     * 是否包含会话
     * @param clientId 客户端id
     * @return 是否存在
     */
    boolean contain(String clientId);

}
