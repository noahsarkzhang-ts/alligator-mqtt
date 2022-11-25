package org.noahsrk.mqtt.broker.server.core;

import org.noahsrk.mqtt.broker.server.core.bean.MqttPublishInnerMessage;

/**
 * Mqtt消息传输类
 *
 * @author zhangxt
 * @date 2022/11/25 10:28
 **/
public interface MqttEventBus {

    void broadcast(MqttPublishInnerMessage msg);
}
