package org.noahsrk.mqtt.broker.server.context;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsrk.mqtt.broker.server.protocol.Will;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MqttSession Factory
 *
 * @author zhangxt
 * @date 2022/10/25 20:12
 **/
public class SessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

    private final ConcurrentMap<String, MqttSession> pool = new ConcurrentHashMap<>();

    private static class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    public void bindToSession(MqttConnection conn, MqttConnectMessage msg, String clientId) {

        if (!pool.containsKey(clientId)) {
            // case 1
            final MqttSession newSession = createNewSession(conn, msg, clientId);

            // publish the session
            final MqttSession previous = pool.putIfAbsent(clientId, newSession);
            final boolean success = previous == null;

            if (success) {
                LOG.trace("case 1, not existing session with CId {}", clientId);
            }
        } else {
            LOG.trace("Existing session with CId {}", clientId);
        }

    }

    private MqttSession createNewSession(MqttConnection mqttConnection, MqttConnectMessage msg, String clientId) {
        final boolean clean = msg.variableHeader().isCleanSession();
        final MqttSession newSession;
        if (msg.variableHeader().isWillFlag()) {
            final Will will = createWill(msg);
            newSession = new MqttSession(clientId,mqttConnection,clean, will);
        } else {
            newSession = new MqttSession(clientId,mqttConnection,clean, null);
        }

        newSession.markConnected();

        return newSession;
    }

    private Will createWill(MqttConnectMessage msg) {
        final ByteBuf willPayload = Unpooled.copiedBuffer(msg.payload().willMessageInBytes());
        final String willTopic = msg.payload().willTopic();
        final boolean retained = msg.variableHeader().isWillRetain();
        final MqttQoS qos = MqttQoS.valueOf(msg.variableHeader().willQos());
        return new Will(willTopic, willPayload, qos, retained);
    }

    public MqttSession retrieve(String clientID) {
        return pool.get(clientID);
    }

    public void remove(String clientID) {
        pool.remove(clientID);
    }

    public void disconnect(String clientID) {
        final MqttSession session = retrieve(clientID);
        if (session == null) {
            LOG.debug("Some other thread already removed the session CId={}", clientID);
            return;
        }
        session.disconnect();
    }


}
