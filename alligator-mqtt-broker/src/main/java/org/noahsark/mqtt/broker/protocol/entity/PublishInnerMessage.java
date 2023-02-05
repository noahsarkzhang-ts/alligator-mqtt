package org.noahsark.mqtt.broker.protocol.entity;

/**
 * Mqtt 消息
 *
 * @author zhangxt
 * @date 2022/11/25 10:50
 **/
public class PublishInnerMessage implements EnqueuedMessage {

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 是否是 Retain 数据
     */
    private boolean retain;

    /**
     *  qos 级别
     */
    private int qos;

    /**
     * 消息内容
     */
    private byte[] payload;

    /**
     * 消息的 packageId
     */
    private int messageId;

    /**
     * 产生的时间戳
     */
    private long timestamp;

    public PublishInnerMessage() {
    }

    public PublishInnerMessage(String topic, boolean retain, int qos, byte[] payload) {
        this.topic = topic;
        this.retain = retain;
        this.qos = qos;
        this.payload = payload;
    }

    public String  getTopic() {
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PublishInnerMessage{" +
                "topic='" + topic + '\'' +
                ", addRetainMessage=" + retain +
                ", qos=" + qos +
                ", messageId=" + messageId +
                ", timestamp=" + timestamp +
                '}';
    }
}
