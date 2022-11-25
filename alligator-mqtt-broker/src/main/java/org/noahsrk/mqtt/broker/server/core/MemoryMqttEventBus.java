package org.noahsrk.mqtt.broker.server.core;

import org.noahsrk.mqtt.broker.server.core.bean.MqttPublishInnerMessage;

/**
 * 内存版的消息总线
 *
 * @author zhangxt
 * @date 2022/11/25 14:08
 **/
public class MemoryMqttEventBus implements MqttEventBus {



    @Override
    public void broadcast(MqttPublishInnerMessage msg) {

    }
}
