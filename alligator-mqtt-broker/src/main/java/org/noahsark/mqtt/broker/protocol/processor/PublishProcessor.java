package org.noahsark.mqtt.broker.protocol.processor;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import org.noahsark.mqtt.broker.transport.session.MqttConnection;
import org.noahsark.mqtt.broker.transport.session.MqttSession;
import org.noahsark.mqtt.broker.transport.session.SessionManager;
import org.noahsark.mqtt.broker.transport.session.Context;
import org.noahsark.mqtt.broker.protocol.DefaultMqttEngine;
import org.noahsark.mqtt.broker.protocol.MqttEngine;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;
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

        MqttPublishMessage message = (MqttPublishMessage) msg;
        MqttConnection conn = context.getConnection();

        try {

            final String clientId = conn.getClientId();
            MqttSession session = sessionManager.retrieve(clientId);

            final MqttQoS qos = message.fixedHeader().qosLevel();
            final String topicName = message.variableHeader().topicName();

            LOG.info("Processing PUBLISH message. CId={}, topic: {}, messageId: {}, qos: {}", clientId, topicName,
                    message.variableHeader().packetId(), qos);

            // 重发功能待补充 TODO
            final boolean dup = message.fixedHeader().isDup();

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
        } finally {
            ReferenceCountUtil.release(message);
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
