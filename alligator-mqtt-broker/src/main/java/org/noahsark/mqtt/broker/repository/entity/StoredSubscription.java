package org.noahsark.mqtt.broker.repository.entity;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsark.mqtt.broker.protocol.subscription.Subscription;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;

import java.io.Serializable;
import java.util.Objects;

/**
 * Subscription 持久化对象
 *
 * @author zhangxt
 * @date 2023/01/29 14:34
 **/
public class StoredSubscription implements Serializable {

    /**
     * 服务级别
     */
    private int qos;

    /**
     * 客户端id
     */
    private String clientId;

    /**
     * topic 过滤器
     */
    private String topicFilter;

    public StoredSubscription() {
    }

    public StoredSubscription(int qos, String clientId, String topicFilter) {
        this.qos = qos;
        this.clientId = clientId;
        this.topicFilter = topicFilter;
    }

    public static Subscription toSubscription(StoredSubscription storedSubscription) {

        return new Subscription(storedSubscription.getClientId(),
                new Topic(storedSubscription.topicFilter), MqttQoS.valueOf(storedSubscription.getQos()));

    }

    public static StoredSubscription fromSubscription(Subscription subscription) {
        return new StoredSubscription(subscription.getRequestedQos().value(),
                subscription.getClientId(), subscription.getTopicFilter().getRawTopic());
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public void setTopicFilter(String topicFilter) {
        this.topicFilter = topicFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof StoredSubscription)) {
            return false;
        }

        StoredSubscription that = (StoredSubscription) o;
        return getQos() == that.getQos() &&
                getClientId().equals(that.getClientId()) &&
                getTopicFilter().equals(that.getTopicFilter());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getQos(), getClientId(), getTopicFilter());
    }

    @Override
    public String toString() {
        return "StoredSubscription{" +
                "qos=" + qos +
                ", clientId='" + clientId + '\'' +
                ", topicFilter='" + topicFilter + '\'' +
                '}';
    }
}
