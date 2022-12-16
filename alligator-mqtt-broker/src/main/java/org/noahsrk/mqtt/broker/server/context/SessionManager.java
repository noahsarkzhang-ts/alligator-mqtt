package org.noahsrk.mqtt.broker.server.context;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import org.noahsrk.mqtt.broker.server.core.Will;
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

    // 存放会话信息，定期清除功能待补充 TODO
    private final ConcurrentMap<String, MqttSession> pool = new ConcurrentHashMap<>();

    private static class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    public void bindToSession(MqttConnection conn, MqttConnectMessage msg, String clientId, String userName) {
        boolean clean = msg.variableHeader().isCleanSession();

        // case 1 session not existing.
        if (!pool.containsKey(clientId) || clean) {
            // case 1
            final MqttSession newSession = createNewSession(conn, msg, clientId, userName);

            // publish the session
            final MqttSession previous = pool.putIfAbsent(clientId, newSession);
            final boolean success = previous == null;

            if (success) {
                LOG.trace("case 1, not existing session with CId {}", clientId);
            }

            return;
        }

        // case 2 session existing
        MqttSession oldSession = pool.get(clientId);
        if (oldSession.disconnected()) {
            oldSession.closeImmediately();
            oldSession.disconnect();
        }

        copySessionConfig(msg, oldSession);
        oldSession.bind(conn);

        oldSession.markConnected();
    }

    private void copySessionConfig(MqttConnectMessage msg, MqttSession session) {
        final boolean clean = msg.variableHeader().isCleanSession();
        final Will will;
        if (msg.variableHeader().isWillFlag()) {
            will = createWill(msg);
        } else {
            will = null;
        }
        session.update(clean, will);
    }

    private MqttSession createNewSession(MqttConnection mqttConnection, MqttConnectMessage msg,
                                         String clientId, String userName) {
        final boolean clean = msg.variableHeader().isCleanSession();
        final MqttSession newSession;
        if (msg.variableHeader().isWillFlag()) {
            final Will will = createWill(msg);
            newSession = new MqttSession(clientId, userName, mqttConnection, clean, will);
        } else {
            newSession = new MqttSession(clientId, userName, mqttConnection, clean, null);
        }

        newSession.markConnected();

        return newSession;
    }

    private Will createWill(MqttConnectMessage msg) {
        final byte[] willPayload = msg.payload().willMessageInBytes();

        final String willTopic = msg.payload().willTopic();
        final boolean retained = msg.variableHeader().isWillRetain();
        final int qos = msg.variableHeader().willQos();
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
