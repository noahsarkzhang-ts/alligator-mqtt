package org.noahsark.mqtt.broker.clusters;

import org.noahsark.mqtt.broker.clusters.entity.ClusterMessage;
import org.noahsark.mqtt.broker.common.factory.Lifecycle;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;

import java.util.concurrent.TimeUnit;

/**
 * Mqtt消息传输类
 *
 * @author zhangxt
 * @date 2022/11/25 10:28
 **/
public interface MqttEventBus extends Lifecycle {

    void broadcast(ClusterMessage msg);

    void receive(ClusterMessage msg);

    ClusterMessage poll(long timeout, TimeUnit unit) throws InterruptedException;

    void publish2Subscribers(PublishInnerMessage message);
}
