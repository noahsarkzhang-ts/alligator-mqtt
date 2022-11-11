package org.noahsrk.mqtt.broker.server.context;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsrk.mqtt.broker.server.protocol.Will;
import org.noahsrk.mqtt.broker.server.subscription.Subscription;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MQTT MqttSession
 *
 * @author zhangxt
 * @date 2022/10/25 20:03
 **/
public class MqttSession {

    private static final Logger LOG = LoggerFactory.getLogger(MqttSession.class);

    private String clientId;
    private boolean clean;
    private Will will;

    private MqttConnection connection;
    private List<Subscription> subscriptions = new ArrayList<>();
    private final AtomicReference<SessionStatus> status = new AtomicReference<>(SessionStatus.DISCONNECTED);

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
        // TODO
    }

    private void sendPublishQos2(Topic topic, MqttQoS qos, ByteBuf payload) {
        // TODO
    }

    public void receivedPublishQos2(int messageID, MqttPublishMessage msg) {
        //qos2Receiving.put(messageID, msg);
        msg.retain(); // retain to put in the inflight map
        connection.sendPublishReceived(messageID);
    }

    public enum SessionStatus {
        CONNECTED, CONNECTING, DISCONNECTING, DISCONNECTED
    }


}
