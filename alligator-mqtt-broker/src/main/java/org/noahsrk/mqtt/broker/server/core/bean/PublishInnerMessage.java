package org.noahsrk.mqtt.broker.server.core.bean;

import org.noahsrk.mqtt.broker.server.subscription.Topic;

import java.io.Serializable;

/**
 * Mqtt 消息
 *
 * @author zhangxt
 * @date 2022/11/25 10:50
 **/
public class PublishInnerMessage implements EnqueuedMessage {

    private Topic topic;

    private boolean retain;

    private int qos;

    private byte[] payload;

    private int messageId;

    public PublishInnerMessage() {
    }

    public PublishInnerMessage(Topic topic, boolean retain, int qos, byte[] payload) {
        this.topic = topic;
        this.retain = retain;
        this.qos = qos;
        this.payload = payload;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
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

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
}
