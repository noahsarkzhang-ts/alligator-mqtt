package org.noahsrk.mqtt.broker.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
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
 * 内存版消息转发器
 *
 * @author zhangxt
 * @date 2022/11/11 15:48
 **/
public class MemoryEventBusV1 implements EventBus {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryEventBusV1.class);

    private BlockingQueue<PublishedMessage> messages;
    private PermitAllAuthorizator authorizator;
    private SessionManager sessionManager;

    private static final class Holder {
        private static final MemoryEventBusV1 INSTANCE = new MemoryEventBusV1();
    }

    private MemoryEventBusV1() {
        messages = new LinkedBlockingQueue<>();

        authorizator = PermitAllAuthorizator.getInstance();
        sessionManager = SessionManager.getInstance();

    }

    public static MemoryEventBusV1 getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean emit(PublishedMessage message) {

        LOG.info("Receive a PublishedMessage:{},{}", message.getTopic(), message.getPublishingQos());

        return messages.offer(message);
    }

    @Override
    public PublishedMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
        return messages.poll(timeout, unit);
    }

    @Override
    public void publish2Subscribers(PublishedMessage message) {

        LOG.info("Push a PublishedMessage:{},{}", message.getTopic(), message.getPublishingQos());

        ByteBuf origPayload = message.getPayload();
        Topic topic = message.getTopic();
        MqttQoS publishingQos = message.getPublishingQos();

        Set<Subscription> topicMatchingSubscriptions = PostOffice.getInstance().matchQosSharpening(topic);
        LOG.info("Matched Subscription size: {}", topicMatchingSubscriptions.size());

        /*for (final Subscription sub : topicMatchingSubscriptions) {

            LOG.info("Matched Subscription: {}, publishingQos: {}", sub.toString(), publishingQos);

            MqttQoS qos = PostOffice.getInstance().lowerQosToTheSubscriptionDesired(sub, publishingQos);
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
                ByteBuf payload = origPayload.retainedDuplicate();
                targetSession.sendPublishOnSessionAtQos(topic, qos, payload);
            } else {
                // If we are, the subscriber disconnected after the subscriptions tree selected that session as a
                // destination.
                LOG.debug("PUBLISH to not yet present session. CId: {}, topicFilter: {}, qos: {}", sub.getClientId(),
                        sub.getTopicFilter(), qos);
            }
        }*/
    }

}
