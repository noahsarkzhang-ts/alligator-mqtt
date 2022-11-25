package org.noahsrk.mqtt.broker.server.core.bean;

import org.noahsrk.mqtt.broker.server.subscription.Topic;

import java.io.Serializable;

/**
 * Mqtt 消息
 *
 * @author zhangxt
 * @date 2022/11/25 10:50
 **/
public class MqttPublishInnerMessage implements Serializable {

    private String clientId;

    private Topic topic;

    private String userName;

    private boolean retain;

    private int qos;

    private byte[] payload;

    public MqttPublishInnerMessage() {
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
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
