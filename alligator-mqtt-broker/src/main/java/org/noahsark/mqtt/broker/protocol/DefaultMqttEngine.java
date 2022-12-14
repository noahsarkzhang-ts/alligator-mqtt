package org.noahsark.mqtt.broker.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import org.noahsark.mqtt.broker.clusters.ClusterMqttEventBusManager;
import org.noahsark.mqtt.broker.clusters.SingletonEventBus;
import org.noahsark.mqtt.broker.clusters.MqttEventBus;
import org.noahsark.mqtt.broker.clusters.entity.ClusterMessage;
import org.noahsark.mqtt.broker.clusters.entity.ClusterSubscriptionInfo;
import org.noahsark.mqtt.broker.common.factory.MqttBeanFactory;
import org.noahsark.mqtt.broker.common.util.Utils;
import org.noahsark.mqtt.broker.protocol.entity.Will;
import org.noahsark.mqtt.broker.transport.session.MqttSession;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.protocol.entity.RetainedMessage;
import org.noahsark.mqtt.broker.repository.MysqlMessageRepository;
import org.noahsark.mqtt.broker.repository.RetainedRepository;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.protocol.entity.StoredMessage;
import org.noahsark.mqtt.broker.repository.MemoryRetainedRepository;
import org.noahsark.mqtt.broker.repository.MemorySubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.MessageRepository;
import org.noahsark.mqtt.broker.protocol.security.Authorizator;
import org.noahsark.mqtt.broker.protocol.security.PermitAllAuthorizator;
import org.noahsark.mqtt.broker.protocol.subscription.CTrieSubscriptionDirectory;
import org.noahsark.mqtt.broker.protocol.subscription.Subscription;
import org.noahsark.mqtt.broker.protocol.subscription.SubscriptionsDirectory;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.FAILURE;

/**
 * ?????????MqttEngine
 *
 * @author zhangxt
 * @date 2022/11/25 14:05
 **/
public class DefaultMqttEngine implements MqttEngine {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMqttEngine.class);

    private Authorizator authorizator;

    private MqttEventBus eventBus;

    private MessageRepository messageRepository;

    private RetainedRepository retainedRepository;

    private SubscriptionsRepository subscriptionsRepository;

    private SubscriptionsDirectory subscriptionsDirectory;

    private static class Holder {
        private static final DefaultMqttEngine INSTANCE = new DefaultMqttEngine();
    }

    private DefaultMqttEngine() {
        load();
    }

    public void load() {
        authorizator = PermitAllAuthorizator.getInstance();
        eventBus = MqttBeanFactory.getInstance().mqttEventBus();
        messageRepository = new MysqlMessageRepository();

        retainedRepository = MemoryRetainedRepository.getInstance();

        subscriptionsRepository = new MemorySubscriptionsRepository();
        subscriptionsDirectory = new CTrieSubscriptionDirectory();
        subscriptionsDirectory.init(subscriptionsRepository);
    }

    public static MqttEngine getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void receivedPublishQos0(MqttSession session, PublishInnerMessage msg) {

        // 1. ?????? topic
        if (!validateTopic(session, msg)) {
            LOG.info("Topic is invalid!");

            return;
        }

        Topic topic = msg.getTopic();
        if (msg.isRetain()) {
            // QoS == 0 && retain => clean old retained
            retainedRepository.cleanRetained(topic);
        }

        // 2, QOS0 ???????????????????????????????????????
        // MqttPublishMessage --> PublishInnerMessage
        ClusterMessage clusterMessage = new ClusterMessage(ClusterMessage.ClusterMessageType.PUBLISH, msg);
        eventBus.broadcast(clusterMessage);
    }

    @Override
    public void receivedPublishQos1(MqttSession session, PublishInnerMessage msg) {
        // 1. ?????? topic
        if (!validateTopic(session, msg)) {
            LOG.info("Topic is invalid!");

            return;
        }

        // 2, QOS1 ?????????
        // MqttPublishMessage --> StoredMessage
        StoredMessage storedMessage = convertStoredMessage(msg);
        messageRepository.store(storedMessage);

        // 2. ????????????
        // MqttPublishMessage --> PublishInnerMessage
        ClusterMessage clusterMessage = new ClusterMessage(ClusterMessage.ClusterMessageType.PUBLISH, msg);
        eventBus.broadcast(clusterMessage);

        // 3. ?????? ACK
        session.getConnection().sendPubAck(msg.getMessageId());

        // 4. ?????? retain ??????
        processDataRetain(msg);

    }

    @Override
    public void receivedPublishQos2(MqttSession session, PublishInnerMessage msg) {

        // 1. ?????? topic
        if (!validateTopic(session, msg)) {
            LOG.info("Topic is invalid!");
            return;
        }

        // 2. ?????? Session ?????????,?????? PUBREC ??????
        session.receivedPublishQos2(msg);
    }

    /**
     * ?????? topic ????????????
     *
     * @param session ??????
     * @param msg     ??????
     * @return ????????????
     */
    private boolean validateTopic(MqttSession session, PublishInnerMessage msg) {
        // ???????????????
        Topic topic = msg.getTopic();

        // verify if topic can be write
        topic.getTokens();
        if (!topic.isValid()) {
            LOG.warn("Invalid topic format, force close the connection");
            session.getConnection().dropConnection();

            return false;
        }

        final String clientId = session.getClientId();
        if (!authorizator.canWrite(topic, session.getUserName(), clientId)) {
            LOG.error("MQTT client: {} is not authorized to publish on topic: {}", clientId, topic);
            return false;
        }

        return true;
    }

    /**
     * ?????? Retain ??????
     *
     * @param msg PublishInnerMessage
     */
    private void processDataRetain(PublishInnerMessage msg) {
        // 1. case 1: retain = 1 ??? payload ??? null, ?????? retain ??????
        // 2. case2: retain = 1 ??? payload ?????? null???????????? retain ?????????????????? topic???retain ?????????????????????????????????
        Topic topic = msg.getTopic();
        if (msg.isRetain()) {
            if (msg.getPayload() == null || msg.getPayload().length == 0) {
                retainedRepository.cleanRetained(topic);
            } else {
                // before wasn't stored
                retainedRepository.retain(topic, new RetainedMessage(msg.getQos(), msg.getPayload()));
            }
        }
    }

    @Override
    public void receivePubrel(MqttSession session, PublishInnerMessage msg) {
        // 1, QOS1 ?????????
        // MqttPublishMessage --> StoredMessage
        StoredMessage storedMessage = convertStoredMessage(msg);
        messageRepository.store(storedMessage);

        // 2. ??????session????????????
        session.receivedPubRelQos2(msg.getMessageId());

        // 3. ????????????
        ClusterMessage clusterMessage = new ClusterMessage(ClusterMessage.ClusterMessageType.PUBLISH, msg);
        eventBus.broadcast(clusterMessage);

        // 3. ?????? PUBCOMP ??????
        session.getConnection().sendPubCompMessage(msg.getMessageId());

        // 4. ?????? retain ??????
        processDataRetain(msg);
    }

    /**
     * ??? PublishInnerMessage ????????? StoredMessage
     *
     * @param msg PublishInnerMessage
     * @return StoredMessage
     */
    private StoredMessage convertStoredMessage(PublishInnerMessage msg) {

        StoredMessage storedMessage = new StoredMessage();
        storedMessage.setQos(msg.getQos());
        storedMessage.setTopic(msg.getTopic().getRawTopic());
        storedMessage.setPayload(msg.getPayload());

        return storedMessage;
    }


    @Override
    public void subcribe(MqttSession session, MqttSubscribeMessage msg) {

        // verify which topics of the subscribe ongoing has read access permission
        String clientId = session.getClientId();
        String userName = session.getUserName();

        int messageId = Utils.messageId(msg);
        List<MqttTopicSubscription> ackTopics = authorizator.verifyTopicsReadAccess(clientId, userName, msg);

        // 1. ?????????Trie??????????????????????????????
        List<Subscription> newSubscriptions = ackTopics.stream()
                .filter(req -> req.qualityOfService() != FAILURE)
                .map(req -> {
                    final Topic topic = new Topic(req.topicName());
                    return new Subscription(clientId, topic, req.qualityOfService());
                }).collect(Collectors.toList());

        LOG.info("Before Ctrie tree:{}", subscriptionsDirectory.dumpTree());
        Set<String> beforeHeadTokens = subscriptionsDirectory.traverseHeadTokens();

        for (Subscription subscription : newSubscriptions) {
            subscriptionsDirectory.add(subscription);
        }

        Set<String> afterHeadTokens = subscriptionsDirectory.traverseHeadTokens();

        LOG.info("After Ctrie tree:{}", subscriptionsDirectory.dumpTree());

        // 1.1 ??? Session ?????????????????????
        session.addSubscriptions(newSubscriptions);

        // 1.2 ?????? Ack ??????
        MqttSubAckMessage ackMessage = doAckMessageFromValidateFilters(ackTopics, messageId);
        session.getConnection().sendSubAckMessage(messageId, ackMessage);

        // 1.3 ?????? Retained ??????
        publishRetainedMessagesForSubscriptions(session, newSubscriptions);

        // 2. ??????????????????topic???????????????(????????????)
        // TODO
        broadcastSubscription(beforeHeadTokens, afterHeadTokens);
    }

    private void publishRetainedMessagesForSubscriptions(MqttSession session, List<Subscription> newSubscriptions) {

        for (Subscription subscription : newSubscriptions) {
            final String topicFilter = subscription.getTopicFilter().toString();
            final List<RetainedMessage> retainedMsgs = retainedRepository.retainedOnTopic(topicFilter);

            if (retainedMsgs.isEmpty()) {
                // not found
                continue;
            }
            for (RetainedMessage retainedMsg : retainedMsgs) {
                final MqttQoS retainedQos = MqttQoS.valueOf(retainedMsg.getQos());
                MqttQoS qos = lowerQosToTheSubscriptionDesired(subscription, retainedQos);

                final ByteBuf payloadBuf = Unpooled.wrappedBuffer(retainedMsg.getPayload());
                session.sendRetainedPublishOnSessionAtQos(subscription.getTopicFilter(), qos, payloadBuf);
            }
        }
    }

    @Override
    public MqttQoS lowerQosToTheSubscriptionDesired(Subscription sub, MqttQoS qos) {
        if (qos.value() > sub.getRequestedQos().value()) {
            qos = sub.getRequestedQos();
        }
        return qos;
    }

    @Override
    public void unsubscribe(MqttSession session, MqttUnsubscribeMessage msg) {

        // 1. ??????Trie???????????????????????????
        final String clientId = session.getClientId();
        List<String> topics = msg.payload().topics();

        LOG.info("Before Ctrie tree:{}", subscriptionsDirectory.dumpTree());
        Set<String> beforeHeadTokens = subscriptionsDirectory.traverseHeadTokens();

        for (String t : topics) {
            Topic topic = new Topic(t);
            boolean validTopic = topic.isValid();
            if (!validTopic) {
                // close the connection, not valid topicFilter is a protocol violation
                session.getConnection().dropConnection();
                LOG.warn("Topic filter is not valid. CId={}, topics: {}, offending topic filter: {}", clientId,
                        topics, topic);
                return;
            }

            LOG.trace("Removing subscription. CId={}, topic={}", clientId, topic);
            subscriptionsDirectory.removeSubscription(topic, clientId);

            // TODO remove the subscriptions to Session
            //  clientSession.unsubscribeFrom(topic);
        }

        Set<String> afterHeadTokens = subscriptionsDirectory.traverseHeadTokens();

        LOG.info("After Ctrie tree:{}", subscriptionsDirectory.dumpTree());

        // 1.2 ?????? ACK ??????
        int messageId = msg.variableHeader().messageId();
        session.getConnection().sendUnsubAckMessage(topics, clientId, messageId);

        // 2. ??????????????????topic???????????????(????????????????????????????????????)
        // TODO
        broadcastSubscription(beforeHeadTokens, afterHeadTokens);
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

    @Override
    public Set<Subscription> matchQosSharpening(Topic topic) {
        return subscriptionsDirectory.matchQosSharpening(topic);
    }

    @Override
    public void fireWill(Will will) {
        // MQTT 3.1.2.8-17

        PublishInnerMessage publishInnerMessage = new PublishInnerMessage(new Topic(will.getTopic()), will.isRetained(),
                will.getQos(), will.getPayload());

        ClusterMessage clusterMessage = new ClusterMessage(ClusterMessage.ClusterMessageType.PUBLISH, publishInnerMessage);
        eventBus.broadcast(clusterMessage);
    }

    private Set<String> difference(Set<String> firstSet, Set<String> secondSet) {
        Set<String> result = new HashSet<>();

        firstSet.forEach(token -> {
            if (!secondSet.contains(token)) {
                result.add(token);
            }
        });

        return result;
    }

    private ClusterSubscriptionInfo buildSubscriptionMessage(Set<String> beforeSet, Set<String> afterSet) {

        ClusterSubscriptionInfo subscriptionInfo = new ClusterSubscriptionInfo();

        subscriptionInfo.setServerId(MqttBeanFactory.getInstance().mqttEventBusManager().getCurrentServer().getId());
        subscriptionInfo.setAddition(difference(afterSet, beforeSet));
        subscriptionInfo.setRemove(difference(beforeSet, afterSet));

        return subscriptionInfo;

    }

    private void broadcastSubscription(Set<String> beforeSet, Set<String> afterSet) {

        ClusterSubscriptionInfo subscriptionInfo = buildSubscriptionMessage(beforeSet, afterSet);

        if (subscriptionInfo.changed()) {

            ClusterMessage clusterMessage = new ClusterMessage(ClusterMessage.ClusterMessageType.SUBSCRIPTION, subscriptionInfo);
            eventBus.broadcast(clusterMessage);
        }
    }
}
