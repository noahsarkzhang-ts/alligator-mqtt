package org.noahsrk.mqtt.broker.server.core;

import org.noahsrk.mqtt.broker.server.clusters.bean.ClusterMessage;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;

import java.util.concurrent.TimeUnit;

/**
 * Mqtt消息传输类
 *
 * @author zhangxt
 * @date 2022/11/25 10:28
 **/
public interface MqttEventBus {

    void broadcast(ClusterMessage msg);

    void receive(ClusterMessage msg);

    ClusterMessage poll(long timeout, TimeUnit unit) throws InterruptedException;

    void publish2Subscribers(PublishInnerMessage message);
}
