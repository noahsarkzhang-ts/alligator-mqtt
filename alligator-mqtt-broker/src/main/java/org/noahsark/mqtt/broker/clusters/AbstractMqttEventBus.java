package org.noahsark.mqtt.broker.clusters;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.commons.configuration2.Configuration;
import org.noahsark.mqtt.broker.clusters.entity.ClusterMessage;
import org.noahsark.mqtt.broker.protocol.DefaultMqttEngine;
import org.noahsark.mqtt.broker.protocol.security.PermitAllAuthorizator;
import org.noahsark.mqtt.broker.protocol.subscription.Subscription;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;
import org.noahsark.mqtt.broker.transport.session.MqttSession;
import org.noahsark.mqtt.broker.transport.session.SessionManager;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.common.exception.OprationNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * MqttEventBus 抽象类
 *
 * @author zhangxt
 * @date 2022/12/16 15:53
 **/
public abstract class AbstractMqttEventBus implements MqttEventBus {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractMqttEventBus.class);

    private PermitAllAuthorizator authorizator;

    public AbstractMqttEventBus() {
    }

    @Override
    public void init() {
        authorizator = PermitAllAuthorizator.getInstance();
    }

    @Override
    public void broadcast(ClusterMessage msg) {
        throw new OprationNotSupportedException();
    }

    @Override
    public void receive(ClusterMessage msg) {
        throw new OprationNotSupportedException();
    }

    @Override
    public ClusterMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new OprationNotSupportedException();
    }

    @Override
    public void publish2Subscribers(PublishInnerMessage message) {
        LOG.info("Push a PublishedMessage:{},{}", message.getTopic(), message.getQos());

        // ByteBuf origPayload = message.getPayload();
        Topic topic = new Topic(message.getTopic());
        MqttQoS publishingQos = MqttQoS.valueOf(message.getQos());

        Set<Subscription> topicMatchingSubscriptions = DefaultMqttEngine.getInstance().matchQosSharpening(topic);
        LOG.info("Matched Subscription size: {}", topicMatchingSubscriptions.size());

        for (final Subscription sub : topicMatchingSubscriptions) {

            LOG.info("Matched Subscription: {}, publishingQos: {}", sub.toString(), publishingQos);

            MqttQoS qos = DefaultMqttEngine.getInstance().lowerQosToTheSubscriptionDesired(sub, publishingQos);
            MqttSession targetSession = SessionManager.getInstance().retrieve(sub.getClientId());

            boolean isSessionPresent = targetSession != null;
            if (isSessionPresent) {
                LOG.debug("Sending PUBLISH message to active subscriber CId: {}, topicFilter: {}, qos: {}",
                        sub.getClientId(), sub.getTopicFilter(), qos);
                //TODO determine the user bounded to targetSession
                if (!authorizator.canRead(topic, "TODO", sub.getClientId())) {
                    LOG.debug("PermitAllAuthorizator prohibit Client {} to be notified on {}", sub.getClientId(), topic);
                    return;
                }

                // we need to addRetainMessage because duplicate only copy r/w indexes and don't addRetainMessage() causing refCnt = 0
                //ByteBuf payload = Unpooled.wrappedBuffer(message.getPayload());

                targetSession.sendPublishOnSessionAtQos(topic, qos, message.getPayload());
            } else {
                // If we are, the subscriber disconnected after the subscriptions tree selected that session as a
                // destination.
                LOG.debug("PUBLISH to not yet present session. CId: {}, topicFilter: {}, qos: {}", sub.getClientId(),
                        sub.getTopicFilter(), qos);
            }
        }
    }

    @Override
    public void load(Configuration configuration) {
    }

    @Override
    public void startup() {

    }

    @Override
    public void shutdown() {
    }
}
