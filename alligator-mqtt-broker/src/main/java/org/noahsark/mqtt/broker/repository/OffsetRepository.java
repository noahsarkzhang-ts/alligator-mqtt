package org.noahsark.mqtt.broker.repository;

import java.util.Map;

/**
 * Topic Offset Repository类
 *
 * @author zhangxt
 * @date 2023/01/30 10:08
 **/
public interface OffsetRepository {

    /**
     * 添加指定 clientId 下指定 topic 的消费进度
     *
     * @param clientId 客户端id
     * @param topic    消息主题
     * @param offset   消费进度
     */
    void addTopicOffset(String clientId, String topic, long offset);

    /**
     * 更新指定 clientId 下指定 topic 的消费进度
     *
     * @param clientId 客户端id
     * @param topic    消息主题
     * @param offset   消费进度
     */
    void updateTopicOffset(String clientId, String topic, long offset);

    /**
     * 获取指定 clientId 下指定 topic 的消费进度
     *
     * @param clientId 客户端id
     * @param topic    消息主题
     */
    long getTopicOffset(String clientId, String topic);

    /**
     * 获取指定 clientId 下所有 topic 的消费进度
     *
     * @param clientId 客户端id
     * @return 消费进度
     */
    Map<String, Long> getAllTopicOffsets(String clientId);

}
