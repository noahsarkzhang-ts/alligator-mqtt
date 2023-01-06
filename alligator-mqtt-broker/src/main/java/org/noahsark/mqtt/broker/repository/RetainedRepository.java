package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.protocol.entity.RetainedMessage;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;

import java.util.List;

/**
 * Retained 消息持久化类
 *
 * @author zhangxt
 * @date 2022/11/25 10:30
 **/
public interface RetainedRepository {

    void cleanRetained(Topic topic);

    void retain(Topic topic, RetainedMessage msg);

    boolean isEmpty();

    List<RetainedMessage> retainedOnTopic(String topic);
}
