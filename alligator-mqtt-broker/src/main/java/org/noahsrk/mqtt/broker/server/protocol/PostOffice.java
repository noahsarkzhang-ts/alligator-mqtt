package org.noahsrk.mqtt.broker.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.common.Utils;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.noahsrk.mqtt.broker.server.processor.UnsubscribeProcessor;
import org.noahsrk.mqtt.broker.server.security.Authorizator;
import org.noahsrk.mqtt.broker.server.subscription.CTrieSubscriptionDirectory;
import org.noahsrk.mqtt.broker.server.subscription.ISubscriptionsDirectory;
import org.noahsrk.mqtt.broker.server.subscription.MemorySubscriptionsRepository;
import org.noahsrk.mqtt.broker.server.subscription.Subscription;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.EXACTLY_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.FAILURE;

/**
 * 消息转发器
 *
 * @author zhangxt
 * @date 2022/11/11 14:58
 **/
public class PostOffice {

    private static final Logger LOG = LoggerFactory.getLogger(PostOffice.class);

    private Authorizator authorizator;

    private SessionManager sessionManager;

    private ISubscriptionsDirectory subscriptions;

    private EventBus eventBus;

    private IRetainedRepository retainedRepository;

    private static final class Holder {
        private static final PostOffice INSTANCE = new PostOffice();
    }

    private PostOffice() {
        authorizator = Authorizator.getInstance();
        sessionManager = SessionManager.getInstance();

        subscriptions = new CTrieSubscriptionDirectory();
        subscriptions.init(new MemorySubscriptionsRepository());

        eventBus = MemoryEventBus.getInstance();
        retainedRepository = MemoryRetainedRepository.getInstance();

    }

    public static PostOffice getInstance() {
        return Holder.INSTANCE;
    }

    public void subscribeClientToTopics(MqttSubscribeMessage msg, String clientID, String username,
                                        MqttConnection mqttConnection) {
        // verify which topics of the subscribe ongoing has read access permission
        int messageID = Utils.messageId(msg);
        List<MqttTopicSubscription> ackTopics = authorizator.verifyTopicsReadAccess(clientID, username, msg);
        MqttSubAckMessage ackMessage = doAckMessageFromValidateFilters(ackTopics, messageID);

        // store topics subscriptions in session
        List<Subscription> newSubscriptions = ackTopics.stream()
                .filter(req -> req.qualityOfService() != FAILURE)
                .map(req -> {
                    final Topic topic = new Topic(req.topicName());
                    return new Subscription(clientID, topic, req.qualityOfService());
                }).collect(Collectors.toList());

        for (Subscription subscription : newSubscriptions) {
            subscriptions.add(subscription);
        }

        LOG.info("Ctrie tree:{}", subscriptions.dumpTree());

        // add the subscriptions to Session
        MqttSession session = sessionManager.retrieve(clientID);
        session.addSubscriptions(newSubscriptions);

        // send ack message
        mqttConnection.sendSubAckMessage(messageID, ackMessage);

        publishRetainedMessagesForSubscriptions(clientID, newSubscriptions);
    }

    private void publishRetainedMessagesForSubscriptions(String clientID, List<Subscription> newSubscriptions) {
        MqttSession targetSession = this.sessionManager.retrieve(clientID);
        for (Subscription subscription : newSubscriptions) {
            final String topicFilter = subscription.getTopicFilter().toString();
            final List<RetainedMessage> retainedMsgs = retainedRepository.retainedOnTopic(topicFilter);

            if (retainedMsgs.isEmpty()) {
                // not found
                continue;
            }
            for (RetainedMessage retainedMsg : retainedMsgs) {
                final MqttQoS retainedQos = retainedMsg.qosLevel();
                MqttQoS qos = lowerQosToTheSubscriptionDesired(subscription, retainedQos);

                final ByteBuf payloadBuf = Unpooled.wrappedBuffer(retainedMsg.getPayload());
                targetSession.sendRetainedPublishOnSessionAtQos(subscription.getTopicFilter(), qos, payloadBuf);
            }
        }
    }

    public MqttQoS lowerQosToTheSubscriptionDesired(Subscription sub, MqttQoS qos) {
        if (qos.value() > sub.getRequestedQos().value()) {
            qos = sub.getRequestedQos();
        }
        return qos;
    }

    /**
     * Create the SUBACK response from a list of topicFilters
     */
    private MqttSubAckMessage doAckMessageFromValidateFilters(List<MqttTopicSubscription> topicFilters, int messageId) {
        List<Integer> grantedQoSLevels = new ArrayList<>();
        for (MqttTopicSubscription req : topicFilters) {
            grantedQoSLevels.add(req.qualityOfService().value());
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, AT_MOST_ONCE,
                false, 0);
        MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
        return new MqttSubAckMessage(fixedHeader, from(messageId), payload);
    }

    public void unsubscribe(List<String> topics, MqttConnection mqttConnection, int messageId) {
        final String clientID = NettyUtils.clientID(mqttConnection.getChannel());
        for (String t : topics) {
            Topic topic = new Topic(t);
            boolean validTopic = topic.isValid();
            if (!validTopic) {
                // close the connection, not valid topicFilter is a protocol violation
                mqttConnection.dropConnection();
                LOG.warn("Topic filter is not valid. CId={}, topics: {}, offending topic filter: {}", clientID,
                        topics, topic);
                return;
            }

            LOG.trace("Removing subscription. CId={}, topic={}", clientID, topic);
            subscriptions.removeSubscription(topic, clientID);

            // TODO remove the subscriptions to Session
            //  clientSession.unsubscribeFrom(topic);
        }

        // ack the client
        mqttConnection.sendUnsubAckMessage(topics, clientID, messageId);
    }

    public void receivedPublishQos0(Topic topic, String username, String clientID, ByteBuf payload, boolean retain,
                                    MqttPublishMessage msg) {
        if (!authorizator.canWrite(topic, username, clientID)) {
            LOG.error("MQTT client: {} is not authorized to publish on topic: {}", clientID, topic);
            return;
        }

        if (retain) {
            // QoS == 0 && retain => clean old retained
            retainedRepository.cleanRetained(topic);
        }

        PublishedMessage publishedMessage = new PublishedMessage(topic, AT_MOST_ONCE, payload);
        eventBus.emit(publishedMessage);
    }

    public void receivedPublishQos1(MqttConnection connection, Topic topic, String username, ByteBuf payload, int messageID,
                                    boolean retain, MqttPublishMessage msg) {

        // verify if topic can be write
        topic.getTokens();
        if (!topic.isValid()) {
            LOG.warn("Invalid topic format, force close the connection");
            connection.dropConnection();
            return;
        }
        final String clientId = connection.getClientId();
        if (!authorizator.canWrite(topic, username, clientId)) {
            LOG.error("MQTT client: {} is not authorized to publish on topic: {}", clientId, topic);
            return;
        }

        PublishedMessage publishedMessage = new PublishedMessage(topic, AT_LEAST_ONCE, payload);
        eventBus.emit(publishedMessage);

        connection.sendPubAck(messageID);

        if (retain) {
            if (!payload.isReadable()) {
                retainedRepository.cleanRetained(topic);
            } else {
                // before wasn't stored
                retainedRepository.retain(topic, msg);
            }
        }

        // interceptor.notifyTopicPublished(msg, clientId, username);
    }

    public void receivedPublishQos2(MqttConnection connection, MqttPublishMessage mqttPublishMessage, String username) {
        LOG.trace("Processing PUBREL message on connection: {}", connection);
        final Topic topic = new Topic(mqttPublishMessage.variableHeader().topicName());
        final ByteBuf payload = mqttPublishMessage.payload();

        final String clientId = connection.getClientId();
        if (!authorizator.canWrite(topic, username, clientId)) {
            LOG.error("MQTT client is not authorized to publish on topic. CId={}, topic: {}", clientId, topic);
            return;
        }

        final boolean retained = mqttPublishMessage.fixedHeader().isRetain();
        if (retained) {
            if (!payload.isReadable()) {
                retainedRepository.cleanRetained(topic);
            } else {
                // before wasn't stored
                retainedRepository.retain(topic, mqttPublishMessage);
            }
        }

        PublishedMessage publishedMessage = new PublishedMessage(topic, EXACTLY_ONCE, payload);
        eventBus.emit(publishedMessage);

    }

    public void fireWill(Will will) {
        // MQTT 3.1.2.8-17
        PublishedMessage publishedMessage = new PublishedMessage(new Topic(will.getTopic()), will.getQos(),
                will.getPayload());
        eventBus.emit(publishedMessage);
    }

    public Set<Subscription> matchQosSharpening(Topic topic) {
        return subscriptions.matchQosSharpening(topic);
    }

}
