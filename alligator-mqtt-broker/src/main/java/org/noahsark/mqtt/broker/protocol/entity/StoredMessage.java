package org.noahsark.mqtt.broker.protocol.entity;

import java.io.Serializable;

/**
 * 持久化的消息类
 *
 * @author zhangxt
 * @date 2022/11/25 10:13
 **/
public class StoredMessage implements Serializable {

    private int id;
    private String topic;
    private int qos;
    private byte[] payload;

    public StoredMessage() {
    }

    public StoredMessage(int id, String topic, int qos, byte[] payload) {
        this.id = id;
        this.topic = topic;
        this.qos = qos;
        this.payload = payload;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
