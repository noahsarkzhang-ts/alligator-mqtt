package org.noahsark.mqtt.broker.clusters.entity;

/**
 * 用于集群版通信的Publish消息
 *
 * @author zhangxt
 * @date 2022/12/19 11:11
 **/
public class ClusterPublishInnerInfo {

    private String topic;

    private boolean retain;

    private int qos;

    private byte[] payload;

    private int messageId;

    public ClusterPublishInnerInfo() {
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
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

    @Override
    public String toString() {
        return "ClusterPublishInnerInfo{" +
                "topic='" + topic + '\'' +
                ", addRetainMessage=" + retain +
                ", qos=" + qos +
                ", messageId=" + messageId +
                '}';
    }
}
