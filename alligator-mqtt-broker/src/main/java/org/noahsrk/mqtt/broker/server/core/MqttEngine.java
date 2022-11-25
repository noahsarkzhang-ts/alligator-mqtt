package org.noahsrk.mqtt.broker.server.core;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import org.noahsrk.mqtt.broker.server.context.MqttSession;

/**
 * Mqtt 中枢处理类
 *
 * @author zhangxt
 * @date 2022/11/25 10:27
 **/
public interface MqttEngine {

    void receivedPublishQos0(MqttSession session, MqttPublishMessage msg);

    void receivedPublishQos1(MqttSession session, MqttPublishMessage msg);

    void receivedPublishQos2(MqttSession session, MqttPublishMessage msg);

    void subcribe(MqttSession session, MqttSubscribeMessage msg);

    void unsubcribe(MqttSession session, MqttUnsubscribeMessage msg);
}
