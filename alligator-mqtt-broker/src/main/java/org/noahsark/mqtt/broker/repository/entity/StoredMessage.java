package org.noahsark.mqtt.broker.repository.entity;

import java.io.Serializable;

/**
 * 持久化的消息类
 *
 * @author zhangxt
 * @date 2022/11/25 10:13
 **/
public class StoredMessage implements Serializable {

    /**
     * 逻辑主键，自增id
     */
    private long id;

    /**
     * 消息id
     */
    private int packageId;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 服务级别
     */
    private int qos;

    /**
     * 消息内容
     */
    private byte[] payload;

    /**
     * 全局id
     */
    private long offset;

    public StoredMessage() {
    }

    public long getId() {
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

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    @Override
    public String toString() {
        return "StoredMessage{" +
                "id=" + id +
                ", packageId=" + packageId +
                ", topic='" + topic + '\'' +
                ", qos=" + qos +
                ", offset=" + offset +
                '}';
    }
}
