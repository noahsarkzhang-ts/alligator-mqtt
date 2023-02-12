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

    /**
     * 向集群广播消息
     * @param msg 消息
     */
    void broadcast(ClusterMessage msg);

    /**
     * 接收集群消息
     * @param msg 消息
     */
    void receive(ClusterMessage msg);

    /**
     * 以阻塞方式获取消息
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 消息
     * @throws InterruptedException 异常
     */
    ClusterMessage poll(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 向本节点客户端推送消息
     * @param message 消息
     */
    void publish2Subscribers(PublishInnerMessage message);
}
