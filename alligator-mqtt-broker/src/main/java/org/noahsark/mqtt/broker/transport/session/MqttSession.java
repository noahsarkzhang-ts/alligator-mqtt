package org.noahsark.mqtt.broker.transport.session;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsark.mqtt.broker.common.factory.MqttModuleFactory;
import org.noahsark.mqtt.broker.common.util.NettyUtils;
import org.noahsark.mqtt.broker.protocol.DefaultMqttEngine;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.protocol.entity.Will;
import org.noahsark.mqtt.broker.protocol.subscription.Subscription;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;
import org.noahsark.mqtt.broker.repository.InflightMessageRepository;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredSubscription;
import org.noahsark.mqtt.broker.repository.factory.CacheBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MQTT MqttSession
 *
 * @author zhangxt
 * @date 2022/10/25 20:03
 **/
public class MqttSession {

    private static final Logger LOG = LoggerFactory.getLogger(MqttSession.class);

    private static final int FLIGHT_BEFORE_RESEND_MS = 5_000;
    private static final int INFLIGHT_WINDOW_SIZE = 10;

    // 会话状态
    private final AtomicReference<MqttSessionStatus> status = new AtomicReference<>(MqttSessionStatus.DISCONNECTED);

    private CacheBeanFactory cacheBeanFactory;

    // 缓存发送中或接收中的消息
    private InflightMessageRepository inflightRepository;

    private DelayQueue<InFlightPacket> inflightTimeouts = new DelayQueue<>();

    // 发送未确认的消息窗口大小
    private AtomicInteger inflightSlots = new AtomicInteger(INFLIGHT_WINDOW_SIZE);

    // 会话的订阅关系
    private SubscriptionsRepository subscriptionsRepository;

    // 会话所属的客户端 id
    private String clientId;

    // 用户名称
    private String userName;

    // 会话是否保持
    private boolean clean;

    // 会话的遗嘱信息
    private Will will;

    // 客户端连接对象
    private MqttConnection connection;

    /**
     * 创建时间
     */
    private long timestamp;

    public MqttSession(MqttConnection connection) {
        init();

        this.connection = connection;
    }

    public MqttSession(String clientId, String userName, MqttConnection connection, boolean clean, Will will) {
        init();

        this.userName = userName;
        this.clientId = clientId;
        this.connection = connection;
        this.clean = clean;
        this.will = will;

        timestamp = System.currentTimeMillis();
    }

    private void init() {
        cacheBeanFactory = MqttModuleFactory.getInstance().cacheBeanFactory();

        inflightRepository = cacheBeanFactory.inflightMessageRepository();
        subscriptionsRepository = cacheBeanFactory.subscriptionsRepository();

    }

    public void markConnected() {
        assignState(MqttSessionStatus.DISCONNECTED, MqttSessionStatus.CONNECTED);
    }

    public void disconnect() {
        final boolean res = assignState(MqttSessionStatus.CONNECTED, MqttSessionStatus.DISCONNECTING);
        if (!res) {
            // someone already moved away from CONNECTED
            // TODO what to do?
            return;
        }

        connection = null;
        will = null;

        assignState(MqttSessionStatus.DISCONNECTING, MqttSessionStatus.DISCONNECTED);
    }

    public void addSubscriptions(List<Subscription> newSubscriptions) {
        newSubscriptions.forEach(subscription -> {
            subscriptionsRepository.addSubscription(clientId, StoredSubscription.fromSubscription(subscription));
        });

    }

    public void removeSubscription(String topic) {
        subscriptionsRepository.removeSubscription(clientId, topic);
    }

    public void sendPublishOnSessionAtQos(Topic topic, MqttQoS qos, byte[] payload) {
        switch (qos) {
            case AT_MOST_ONCE:
                if (connection.isConnected()) {
                    connection.sendPublishNotRetainedQos0(topic, qos, payload);
                }
                break;
            case AT_LEAST_ONCE:
                sendPublishQos1(topic, qos, payload);
                break;
            case EXACTLY_ONCE:
                sendPublishQos2(topic, qos, payload);
                break;
            case FAILURE:
                LOG.error("Not admissible");
        }
    }

    private void sendPublishQos1(Topic topic, MqttQoS qos, byte[] payload) {
        if (!connected() && isClean()) {
            //pushing messages to disconnected not clean session
            return;
        }

        sendPublishMoreThan1(topic, qos, payload);
    }

    private void sendPublishQos2(Topic topic, MqttQoS qos, byte[] payload) {
        sendPublishMoreThan1(topic, qos, payload);
    }

    private void sendPublishMoreThan1(Topic topic, MqttQoS qos, byte[] payload) {
        // TODO
        if (canSkipQueue()) {
            inflightSlots.decrementAndGet();
            int packetId = connection.nextPacketId();

            PublishInnerMessage publishInnerMessage = new PublishInnerMessage(topic.getRawTopic(), false, qos.value(), payload);
            publishInnerMessage.setMessageId(packetId);
            publishInnerMessage.setTimestamp(System.currentTimeMillis());

            inflightRepository.addMessage(clientId, publishInnerMessage, true);
            inflightTimeouts.add(new InFlightPacket(packetId, publishInnerMessage.getTimestamp() + FLIGHT_BEFORE_RESEND_MS));

            ByteBuf messagePayload = Unpooled.wrappedBuffer(payload);
            MqttPublishMessage publishMsg = connection.notRetainedPublishWithMessageId(topic.toString(), qos,
                    messagePayload, packetId);
            connection.sendPublish(publishMsg);

            //drainQueueToConnection();
        } else {
            // TODO
            // 存入本地队列
        }
    }

    public void receivedPublishQos2(PublishInnerMessage msg) {
        inflightRepository.addMessage(clientId, msg, false);
        connection.sendPublishReceived(msg.getMessageId());
    }

    public boolean canSkipQueue() {
        // TODO
        return true;
    }

    public void pubAckReceived(int ackPacketId) {
        // TODO remain to invoke in somehow m_interceptor.notifyMessageAcknowledged
        inflightRepository.removeMessage(clientId, ackPacketId, true);
        inflightSlots.incrementAndGet();

        // TODO 处理队列中的数据
        // drainQueueToConnection();
    }

    public void resendInflightNotAcked() {
        Collection<InFlightPacket> expired = new ArrayList<>(INFLIGHT_WINDOW_SIZE);
        inflightTimeouts.drainTo(expired);

        debugLogPacketIds(expired);

        for (InFlightPacket notAckPacketId : expired) {
            if (inflightRepository.contain(clientId, notAckPacketId.packetId, true)) {
                final PublishInnerMessage msg = inflightRepository.getMessage(clientId, notAckPacketId.packetId, true);
                final String topic = msg.getTopic();
                final MqttQoS qos = MqttQoS.valueOf(msg.getQos());
                final ByteBuf payload = Unpooled.wrappedBuffer(msg.getPayload());
                final ByteBuf copiedPayload = payload.retainedDuplicate();
                MqttPublishMessage publishMsg = publishNotRetainedDuplicated(notAckPacketId, topic, qos, copiedPayload);
                connection.sendPublish(publishMsg);
            }
        }
    }

    public MqttSessionStatus getStatus() {
        return status.get();
    }

    public void resetStatus(int vlaue) {
        status.set(MqttSessionStatus.valueOf(vlaue));
    }


    private boolean assignState(MqttSessionStatus expected, MqttSessionStatus newState) {
        return status.compareAndSet(expected, newState);
    }

    private MqttPublishMessage publishNotRetainedDuplicated(InFlightPacket notAckPacketId, String topic, MqttQoS qos,
                                                            ByteBuf payload) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, true, qos, false, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, notAckPacketId.packetId);
        return new MqttPublishMessage(fixedHeader, varHeader, payload);
    }

    private void debugLogPacketIds(Collection<InFlightPacket> expired) {
        if (!LOG.isDebugEnabled() || expired.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (InFlightPacket packet : expired) {
            sb.append(packet.packetId).append(", ");
        }
        LOG.debug("Resending {} in flight packets [{}]", expired.size(), sb);
    }

    public void processPubRec(int packetId) {
        //inflightWindow.remove(packetId);
        inflightRepository.removeMessage(clientId, packetId, true);
        inflightSlots.incrementAndGet();
        if (canSkipQueue()) {
            inflightSlots.decrementAndGet();
            int pubRelPacketId = packetId/*mqttConnection.nextPacketId()*/;
            inflightRepository.addPubRel(clientId, pubRelPacketId);
            // inflightWindow.put(pubRelPacketId, new PubRelMarker());
            inflightTimeouts.add(new InFlightPacket(pubRelPacketId,
                    System.currentTimeMillis() + FLIGHT_BEFORE_RESEND_MS));
            MqttMessage pubRel = connection.pubrel(pubRelPacketId);
            connection.sendIfWritableElseDrop(pubRel);

            // drainQueueToConnection();
        } else {
            // sessionQueue.add(new SessionRegistry.PubRelMarker());
        }
    }

    public void rebuildInflightTimoutQueueFromCache() {
        List<PublishInnerMessage> inflightMessages = inflightRepository.getAllMessages(clientId, true);

        inflightMessages.forEach(msg -> inflightTimeouts.add(new InFlightPacket(msg.getMessageId(), msg.getTimestamp())));
    }

    public void resubscribeFromCache() {

        DefaultMqttEngine.getInstance().subscribe(getAllSubscriptions());
    }

    public void cleanSubscriptions() {

        DefaultMqttEngine.getInstance().unsubscribe(getAllSubscriptions());

        subscriptionsRepository.clean(clientId);
    }

    public void cleanInflightMsg() {
        inflightRepository.clean(clientId);
    }

    public List<Subscription> getAllSubscriptions() {
        List<StoredSubscription> subscriptions = subscriptionsRepository.getAllSubscriptions(clientId);

        List<Subscription> list = new ArrayList<>();

        subscriptions.forEach(subscription -> {
            list.add(new Subscription(subscription.getClientId(), new Topic(subscription.getTopicFilter()),
                    MqttQoS.valueOf(subscription.getQos())));
        });

        return list;
    }

    public void receivedPubRelQos2(int messageID) {
        // qos2Receiving.remove(messageID);
        inflightRepository.removeMessage(clientId, messageID, false);
    }

    public PublishInnerMessage retrieveMsgQos2(int messageID) {

        return inflightRepository.getMessage(clientId, messageID, false);
    }

    public void processPubComp(int messageID) {
        // inflightWindow.remove(messageID);

        inflightRepository.removePubRel(clientId, messageID);
        inflightSlots.incrementAndGet();

        //drainQueueToConnection();
    }

    public void sendRetainedPublishOnSessionAtQos(Topic topic, MqttQoS qos, ByteBuf payload) {
        if (qos != MqttQoS.AT_MOST_ONCE) {
            // QoS 1 or 2
            connection.sendPublishRetainedWithPacketId(topic, qos, payload);
        } else {
            connection.sendPublishRetainedQos0(topic, qos, payload);
        }
    }

    public void closeImmediately() {
        NettyUtils.connection(connection.getChannel(), null);
        connection.dropConnection();
    }

    public void update(boolean clean, Will will) {
        this.clean = clean;
        this.will = will;
    }

    public void bind(MqttConnection connection) {
        this.connection = connection;
    }

    public boolean hasWill() {
        return will != null;
    }

    public Will getWill() {
        return will;
    }

    public boolean disconnected() {
        return status.get() == MqttSessionStatus.DISCONNECTED;
    }

    public boolean connected() {
        return status.get() == MqttSessionStatus.CONNECTED;
    }

    public boolean isClean() {
        return clean;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public MqttConnection getConnection() {
        return connection;
    }

    private static class InFlightPacket implements Delayed {

        private int packetId;
        private long startTime;

        InFlightPacket(int packetId, long timoutInMilliseconds) {
            this.packetId = packetId;
            this.startTime = timoutInMilliseconds;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = startTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if ((this.startTime - ((InFlightPacket) o).startTime) == 0) {
                return 0;
            }
            if ((this.startTime - ((InFlightPacket) o).startTime) > 0) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public enum MqttSessionStatus {
        CONNECTED(0), DISCONNECTING(1), DISCONNECTED(2);

        private int value;

        MqttSessionStatus(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static MqttSessionStatus valueOf(int value) {
            switch (value) {
                case 0:
                    return CONNECTED;
                case 1:
                    return DISCONNECTING;
                case 2:
                    return DISCONNECTED;
                default:
                    throw new IllegalArgumentException("invalid MqttSessionStatus: " + value);
            }
        }

    }


}
