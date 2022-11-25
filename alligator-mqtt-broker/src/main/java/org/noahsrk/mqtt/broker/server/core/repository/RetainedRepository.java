package org.noahsrk.mqtt.broker.server.core.repository;

import org.noahsrk.mqtt.broker.server.core.bean.RetainedMessage;
import org.noahsrk.mqtt.broker.server.subscription.Topic;

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
