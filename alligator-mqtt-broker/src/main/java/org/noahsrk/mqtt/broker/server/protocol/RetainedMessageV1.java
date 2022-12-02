package org.noahsrk.mqtt.broker.server.protocol;

import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.Serializable;

public class RetainedMessageV1 implements Serializable{

    private final MqttQoS qos;
    private final byte[] payload;

    public RetainedMessageV1(MqttQoS qos, byte[] payload) {
        this.qos = qos;
        this.payload = payload;
    }

    public MqttQoS qosLevel() {
        return qos;
    }

    public byte[] getPayload() {
        return payload;
    }
}
