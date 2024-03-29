package org.noahsark.mqtt.broker.protocol.entity;

/**
 * 遗嘱信息
 *
 * @author zhangxt
 * @date 2022/11/11 11:52
 **/
public class Will {
    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息内容
     */
    private byte [] payload;

    /**
     * QoS 级别
     */
    private int qos;

    /**
     * 是否是 retain 数据
     */
    private boolean retained;

    public Will(String topic, byte [] payload, int qos, boolean retained) {
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

    public byte [] getPayload() {
        return payload;
    }

    public void setPayload(byte [] payload) {
        this.payload = payload;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
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
