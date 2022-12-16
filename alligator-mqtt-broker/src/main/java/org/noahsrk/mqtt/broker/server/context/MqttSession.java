package org.noahsrk.mqtt.broker.server.context;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.core.bean.EnqueuedMessage;
import org.noahsrk.mqtt.broker.server.core.bean.PubRelMarker;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.noahsrk.mqtt.broker.server.core.Will;
import org.noahsrk.mqtt.broker.server.subscription.Subscription;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // 存放发送的 QOS1&2 的 PublishMessage 及 PubRel 数据
    private final Map<Integer, EnqueuedMessage> inflightWindow = new HashMap<>();
    // 按照发送的时间对数据包进行排序
    private final DelayQueue<InFlightPacket> inflightTimeouts = new DelayQueue<>();
    // 发送未确认的消息窗口大小
    private final AtomicInteger inflightSlots = new AtomicInteger(INFLIGHT_WINDOW_SIZE);

    // 存放收到的 QOS2 的 PublishMessage 数据
    private final Map<Integer, PublishInnerMessage> qos2Receiving = new HashMap<>();

    // 会话状态
    private final AtomicReference<MqttSessionStatus> status = new AtomicReference<>(MqttSessionStatus.DISCONNECTED);

    // 会话的订阅关系
    private List<Subscription> subscriptions = new ArrayList<>();

    public MqttSession(MqttConnection connection) {
        this.connection = connection;
    }

    public MqttSession(String clientId, String userName, MqttConnection connection, boolean clean, Will will) {
        this.userName = userName;
        this.clientId = clientId;
        this.connection = connection;
        this.clean = clean;
        this.will = will;
    }

    public void markConnected() {
        assignState(MqttSessionStatus.DISCONNECTED, MqttSessionStatus.CONNECTED);
    }

    private boolean assignState(MqttSessionStatus expected, MqttSessionStatus newState) {
        return status.compareAndSet(expected, newState);
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
        subscriptions.addAll(newSubscriptions);
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

        sendPublishMoreThan1(topic, qos,payload);
    }

    private void sendPublishQos2(Topic topic, MqttQoS qos, byte[] payload) {
        sendPublishMoreThan1(topic, qos,payload);
    }

    private void sendPublishMoreThan1(Topic topic, MqttQoS qos, byte[] payload) {
        // TODO
        if (canSkipQueue()) {
            inflightSlots.decrementAndGet();
            int packetId = connection.nextPacketId();
            inflightWindow.put(packetId, new PublishInnerMessage(topic, false, qos.value(), payload));
            inflightTimeouts.add(new InFlightPacket(packetId, FLIGHT_BEFORE_RESEND_MS));

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
        qos2Receiving.put(msg.getMessageId(), msg);
        connection.sendPublishReceived(msg.getMessageId());
    }

    public boolean canSkipQueue() {
        // TODO
        return true;
    }

    public void pubAckReceived(int ackPacketId) {
        // TODO remain to invoke in somehow m_interceptor.notifyMessageAcknowledged
        inflightWindow.remove(ackPacketId);
        inflightSlots.incrementAndGet();

        // TODO 处理队列中的数据
        // drainQueueToConnection();
    }

    public void resendInflightNotAcked() {
        Collection<InFlightPacket> expired = new ArrayList<>(INFLIGHT_WINDOW_SIZE);
        inflightTimeouts.drainTo(expired);

        debugLogPacketIds(expired);

        for (InFlightPacket notAckPacketId : expired) {
            if (inflightWindow.containsKey(notAckPacketId.packetId)) {
                final PublishInnerMessage msg = (PublishInnerMessage) inflightWindow.get(notAckPacketId.packetId);
                final Topic topic = msg.getTopic();
                final MqttQoS qos = MqttQoS.valueOf(msg.getQos());
                final ByteBuf payload = Unpooled.wrappedBuffer(msg.getPayload());
                final ByteBuf copiedPayload = payload.retainedDuplicate();
                MqttPublishMessage publishMsg = publishNotRetainedDuplicated(notAckPacketId, topic, qos, copiedPayload);
                connection.sendPublish(publishMsg);
            }
        }
    }

    private MqttPublishMessage publishNotRetainedDuplicated(InFlightPacket notAckPacketId, Topic topic, MqttQoS qos,
                                                            ByteBuf payload) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, true, qos, false, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic.toString(), notAckPacketId.packetId);
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
        inflightWindow.remove(packetId);
        inflightSlots.incrementAndGet();
        if (canSkipQueue()) {
            inflightSlots.decrementAndGet();
            int pubRelPacketId = packetId/*mqttConnection.nextPacketId()*/;
            inflightWindow.put(pubRelPacketId, new PubRelMarker());
            inflightTimeouts.add(new InFlightPacket(pubRelPacketId, FLIGHT_BEFORE_RESEND_MS));
            MqttMessage pubRel = connection.pubrel(pubRelPacketId);
            connection.sendIfWritableElseDrop(pubRel);

            // drainQueueToConnection();
        } else {
            // sessionQueue.add(new SessionRegistry.PubRelMarker());
        }
    }

    public void receivedPubRelQos2(int messageID) {
        qos2Receiving.remove(messageID);
    }

    public PublishInnerMessage retrieveMsgQos2(int messageID) {
        return qos2Receiving.get(messageID);
    }

    public void processPubComp(int messageID) {
        inflightWindow.remove(messageID);
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

        final int packetId;
        private long startTime;

        InFlightPacket(int packetId, long delayInMilliseconds) {
            this.packetId = packetId;
            this.startTime = System.currentTimeMillis() + delayInMilliseconds;
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
        CONNECTED, DISCONNECTING, DISCONNECTED
    }


}
