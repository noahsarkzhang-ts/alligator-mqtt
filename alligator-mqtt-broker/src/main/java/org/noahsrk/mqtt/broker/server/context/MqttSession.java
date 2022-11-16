package org.noahsrk.mqtt.broker.server.context;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.protocol.EnqueuedMessage;
import org.noahsrk.mqtt.broker.server.protocol.PubRelMarker;
import org.noahsrk.mqtt.broker.server.protocol.PublishedMessage;
import org.noahsrk.mqtt.broker.server.protocol.Will;
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

    private String clientId;
    private boolean clean;
    private Will will;

    private MqttConnection connection;
    private List<Subscription> subscriptions = new ArrayList<>();
    private final Map<Integer, EnqueuedMessage> inflightWindow = new HashMap<>();
    private final DelayQueue<InFlightPacket> inflightTimeouts = new DelayQueue<>();

    private final Map<Integer, MqttPublishMessage> qos2Receiving = new HashMap<>();

    private final AtomicReference<SessionStatus> status = new AtomicReference<>(SessionStatus.DISCONNECTED);
    private final AtomicInteger inflightSlots = new AtomicInteger(INFLIGHT_WINDOW_SIZE);

    public MqttSession(MqttConnection connection) {
        this.connection = connection;
    }

    public MqttSession(String clientId, MqttConnection connection, boolean clean, Will will) {
        this.clientId = clientId;
        this.connection = connection;
        this.clean = clean;
        this.will = will;
    }

    public void markConnected() {
        assignState(SessionStatus.DISCONNECTED, SessionStatus.CONNECTED);
    }

    private boolean assignState(SessionStatus expected, SessionStatus newState) {
        return status.compareAndSet(expected, newState);
    }

    public void disconnect() {
        final boolean res = assignState(SessionStatus.CONNECTED, SessionStatus.DISCONNECTING);
        if (!res) {
            // someone already moved away from CONNECTED
            // TODO what to do?
            return;
        }

        connection = null;
        will = null;

        assignState(SessionStatus.DISCONNECTING, SessionStatus.DISCONNECTED);
    }

    public void addSubscriptions(List<Subscription> newSubscriptions) {
        subscriptions.addAll(newSubscriptions);
    }

    public void sendPublishOnSessionAtQos(Topic topic, MqttQoS qos, ByteBuf payload) {
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

    private void sendPublishQos1(Topic topic, MqttQoS qos, ByteBuf payload) {
        if (!connected() && isClean()) {
            //pushing messages to disconnected not clean session
            return;
        }

        if (canSkipQueue()) {
            inflightSlots.decrementAndGet();
            int packetId = connection.nextPacketId();
            inflightWindow.put(packetId, new PublishedMessage(topic, qos, payload));
            inflightTimeouts.add(new InFlightPacket(packetId, FLIGHT_BEFORE_RESEND_MS));
            MqttPublishMessage publishMsg = connection.notRetainedPublishWithMessageId(topic.toString(), qos,
                    payload, packetId);
            connection.sendPublish(publishMsg);

            // TODO drainQueueToConnection();?
        } else {

            // TODO
            // 存入本地队列
        }
    }

    private void sendPublishQos2(Topic topic, MqttQoS qos, ByteBuf payload) {
        // TODO
        if (canSkipQueue()) {
            inflightSlots.decrementAndGet();
            int packetId = connection.nextPacketId();
            inflightWindow.put(packetId, new PublishedMessage(topic, qos, payload));
            inflightTimeouts.add(new InFlightPacket(packetId, FLIGHT_BEFORE_RESEND_MS));
            MqttPublishMessage publishMsg = connection.notRetainedPublishWithMessageId(topic.toString(), qos,
                    payload, packetId);
            connection.sendPublish(publishMsg);

            //drainQueueToConnection();
        } else {
            // TODO
            // 存入本地队列
        }
    }

    public void receivedPublishQos2(int messageID, MqttPublishMessage msg) {
        qos2Receiving.put(messageID, msg);
        msg.retain(); // retain to put in the inflight map
        connection.sendPublishReceived(messageID);
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
                final PublishedMessage msg = (PublishedMessage) inflightWindow.get(notAckPacketId.packetId);
                final Topic topic = msg.getTopic();
                final MqttQoS qos = msg.getPublishingQos();
                final ByteBuf payload = msg.getPayload();
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
        final MqttPublishMessage removedMsg = qos2Receiving.remove(messageID);
        ReferenceCountUtil.release(removedMsg);
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
        return status.get() == SessionStatus.DISCONNECTED;
    }

    public boolean connected() {
        return status.get() == SessionStatus.CONNECTED;
    }

    public boolean isClean() {
        return clean;
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

    public enum SessionStatus {
        CONNECTED, DISCONNECTING, DISCONNECTED
    }


}
