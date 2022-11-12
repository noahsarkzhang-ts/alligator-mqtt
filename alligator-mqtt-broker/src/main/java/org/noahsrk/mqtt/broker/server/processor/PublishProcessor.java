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

    private PostOffice postOffice = PostOffice.getInstance();

    private SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {
        MqttConnection conn = context.getConnection();
        Channel channel = conn.getChannel();
        MqttPublishMessage message = (MqttPublishMessage)msg;

        final MqttQoS qos = message.fixedHeader().qosLevel();
        final String username = NettyUtils.userName(channel);
        final String topicName = message.variableHeader().topicName();
        final String clientId = conn.getClientId();

        LOG.info("Processing PUBLISH message. CId={}, topic: {}, messageId: {}, qos: {}", clientId, topicName,
                message.variableHeader().packetId(), qos);

        ByteBuf payload = message.payload();
        final boolean retain = message.fixedHeader().isRetain();
        final Topic topic = new Topic(topicName);
        if (!topic.isValid()) {
            LOG.debug("Drop connection because of invalid topic format");
            conn.dropConnection();
        }
        switch (qos) {
            case AT_MOST_ONCE:
                postOffice.receivedPublishQos0(topic, username, clientId, payload, retain, message);
                break;
            case AT_LEAST_ONCE: {
                final int messageID = message.variableHeader().packetId();
                postOffice.receivedPublishQos1(conn, topic, username, payload, messageID, retain, message);
                break;
            }
            case EXACTLY_ONCE: {
                final int messageID = message.variableHeader().packetId();
                final MqttSession session = sessionManager.retrieve(clientId);
                session.receivedPublishQos2(messageID, message);
                postOffice.receivedPublishQos2(conn, message, username);
                break;
            }
            default:
                LOG.error("Unknown QoS-Type:{}", qos);
                break;
        }
    }
}
