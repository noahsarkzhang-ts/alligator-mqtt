package org.noahsrk.mqtt.broker.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.noahsrk.mqtt.broker.server.security.PermitAllAuthorizator;
import org.noahsrk.mqtt.broker.server.subscription.Subscription;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 内存版的消息总线
 *
 * @author zhangxt
 * @date 2022/11/25 14:08
 **/
public class MemoryMqttEventBus implements MqttEventBus {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryMqttEventBus.class);

    private BlockingQueue<PublishInnerMessage> messages;

    private PermitAllAuthorizator authorizator;
    private SessionManager sessionManager;

    private static final class Holder {
        private static final MqttEventBus INSTANCE = new MemoryMqttEventBus();
    }

    private MemoryMqttEventBus() {
        messages = new LinkedBlockingQueue<>();

        authorizator = PermitAllAuthorizator.getInstance();
        sessionManager = SessionManager.getInstance();
    }

    public static MqttEventBus getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void broadcast(PublishInnerMessage msg) {
        LOG.info("Receive a PublishedMessage:{},{}", msg.getTopic(), msg.getQos());

        messages.offer(msg);
    }

    @Override
    public PublishInnerMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
        return messages.poll(timeout, unit);
    }

    @Override
    public void publish2Subscribers(PublishInnerMessage message) {
        LOG.info("Push a PublishedMessage:{},{}", message.getTopic(), message.getQos());

        // ByteBuf origPayload = message.getPayload();
        Topic topic = message.getTopic();
        MqttQoS publishingQos = MqttQoS.valueOf(message.getQos());

        Set<Subscription> topicMatchingSubscriptions = DefaultMqttEngine.getInstance().matchQosSharpening(topic);
        LOG.info("Matched Subscription size: {}", topicMatchingSubscriptions.size());

        for (final Subscription sub : topicMatchingSubscriptions) {

            LOG.info("Matched Subscription: {}, publishingQos: {}", sub.toString(), publishingQos);

            MqttQoS qos = DefaultMqttEngine.getInstance().lowerQosToTheSubscriptionDesired(sub, publishingQos);
            MqttSession targetSession = sessionManager.retrieve(sub.getClientId());

            boolean isSessionPresent = targetSession != null;
            if (isSessionPresent) {
                LOG.debug("Sending PUBLISH message to active subscriber CId: {}, topicFilter: {}, qos: {}",
                        sub.getClientId(), sub.getTopicFilter(), qos);
                //TODO determine the user bounded to targetSession
                if (!authorizator.canRead(topic, "TODO", sub.getClientId())) {
                    LOG.debug("PermitAllAuthorizator prohibit Client {} to be notified on {}", sub.getClientId(), topic);
                    return;
                }

                // we need to retain because duplicate only copy r/w indexes and don't retain() causing refCnt = 0
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


}
