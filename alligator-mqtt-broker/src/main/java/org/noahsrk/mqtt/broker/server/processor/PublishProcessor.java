package org.noahsrk.mqtt.broker.server.processor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.noahsrk.mqtt.broker.server.core.DefaultMqttEngine;
import org.noahsrk.mqtt.broker.server.core.MqttEngine;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.noahsrk.mqtt.broker.server.protocol.PostOffice;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * publish消息处理器
 *
 * @author zhangxt
 * @date 2022/11/11 16:41
 **/
public class PublishProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PublishProcessor.class);

    private MqttEngine mqttEngine = DefaultMqttEngine.getInstance();

    private SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {
        MqttConnection conn = context.getConnection();
        final String clientId = conn.getClientId();

        MqttSession session = sessionManager.retrieve(clientId);
        final String username = session.getUserName();

        MqttPublishMessage message = (MqttPublishMessage) msg;
        final MqttQoS qos = message.fixedHeader().qosLevel();
        final String topicName = message.variableHeader().topicName();

        LOG.info("Processing PUBLISH message. CId={}, topic: {}, messageId: {}, qos: {}", clientId, topicName,
                message.variableHeader().packetId(), qos);

        // 重发功能待补充 TODO
        final boolean dup = message.fixedHeader().isDup();

        ByteBuf payload = message.payload();
        final boolean retain = message.fixedHeader().isRetain();
        final Topic topic = new Topic(topicName);
        if (!topic.isValid()) {
            LOG.debug("Drop connection because of invalid topic format");
            conn.dropConnection();
        }

        PublishInnerMessage publishInnerMessage = convert(message, topic, qos, retain);

        switch (qos) {
            case AT_MOST_ONCE:
                mqttEngine.receivedPublishQos0(session, publishInnerMessage);
                break;
            case AT_LEAST_ONCE: {
                final int messageID = message.variableHeader().packetId();
                publishInnerMessage.setMessageId(messageID);
                mqttEngine.receivedPublishQos1(session, publishInnerMessage);
                break;
            }
            case EXACTLY_ONCE: {
                final int messageID = message.variableHeader().packetId();
                publishInnerMessage.setMessageId(messageID);
                mqttEngine.receivedPublishQos2(session, publishInnerMessage);
                break;
            }
            default:
                LOG.error("Unknown QoS-Type:{}", qos);
                break;
        }
    }

    private PublishInnerMessage convert(MqttPublishMessage msg, Topic topic, MqttQoS qos, boolean retain) {

        PublishInnerMessage message = new PublishInnerMessage();

        final ByteBuf payload = msg.content();
        byte[] rawPayload = new byte[payload.readableBytes()];
        payload.getBytes(0, rawPayload);

        message.setPayload(rawPayload);
        message.setTopic(topic);
        message.setQos(qos.value());
        message.setRetain(retain);

        return message;

    }
}
