package org.noahsrk.mqtt.broker.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * 遗嘱信息
 *
 * @author zhangxt
 * @date 2022/11/11 11:52
 **/
public class Will {
    private String topic;
    private ByteBuf payload;
    private MqttQoS qos;
    private boolean retained;

    public Will(String topic, ByteBuf payload, MqttQoS qos, boolean retained) {
        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retained = retained;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public ByteBuf getPayload() {
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
    }

    public MqttQoS getQos() {
        return qos;
    }

    public void setQos(MqttQoS qos) {
        this.qos = qos;
    }

    public boolean isRetained() {
        return retained;
    }

    public void setRetained(boolean retained) {
        this.retained = retained;
    }

    @Override
    public String toString() {
        return "Will{" +
                "topic='" + topic + '\'' +
                ", payload=" + payload +
                ", qos=" + qos +
                ", retained=" + retained +
                '}';
    }
}
