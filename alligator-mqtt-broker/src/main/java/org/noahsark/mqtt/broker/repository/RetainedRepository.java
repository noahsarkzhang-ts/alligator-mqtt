package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.protocol.entity.RetainedMessage;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;

import java.util.List;

/**
 * Retained 消息 Repository 类
 *
 * @author zhangxt
 * @date 2022/11/25 10:30
 **/
public interface RetainedRepository {

    /**
     * 清空topic下的Retain消息
     *
     * @param topic 消息主题
     */
    void clean(String topic);

    /**
     * 向指定topic下添加Retain消息
     *
     * @param topic 消息主题
     * @param msg   Retain消息
     */
    void addRetainMessage(String topic, RetainedMessage msg);

    /**
     * 获取topic下所有的 Retain消息
     *
     * @param topic 消息主题
     * @return Retain消息列表
     */
    List<RetainedMessage> getAllRetainMessage(String topic);
}
