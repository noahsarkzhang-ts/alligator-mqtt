package org.noahsrk.mqtt.broker.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsrk.mqtt.broker.server.core.bean.EnqueuedMessage;
import org.noahsrk.mqtt.broker.server.subscription.Topic;

/**
 * publish消息
 *
 * @author zhangxt
 * @date 2022/11/11 15:43
 **/
public class PublishedMessage implements EnqueuedMessage {

    private Topic topic;
    private MqttQoS publishingQos;
    private ByteBuf payload;

    public PublishedMessage(Topic topic, MqttQoS publishingQos, ByteBuf payload) {
        this.topic = topic;
        this.publishingQos = publishingQos;
        this.payload = payload;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public MqttQoS getPublishingQos() {
        return publishingQos;
    }

    public void setPublishingQos(MqttQoS publishingQos) {
        this.publishingQos = publishingQos;
    }

    public ByteBuf getPayload() {
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
    }
}
