package org.noahsark.mqtt.broker.protocol.entity;

import java.io.Serializable;

/**
 * Retained message
 *
 * @author zhangxt
 * @date 2022/11/25 10:34
 **/
public class RetainedMessage implements Serializable {

    private int qos;
    private byte[] payload;

    public RetainedMessage(int qos, byte[] payload) {
        this.qos = qos;
        this.payload = payload;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
}
