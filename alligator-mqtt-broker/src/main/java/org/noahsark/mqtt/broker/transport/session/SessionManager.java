package org.noahsark.mqtt.broker.transport.session;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import org.noahsark.mqtt.broker.clusters.MqttEventBus;
import org.noahsark.mqtt.broker.clusters.MqttEventBusManager;
import org.noahsark.mqtt.broker.clusters.entity.ClusterClientLogoutInfo;
import org.noahsark.mqtt.broker.clusters.entity.ClusterMessage;
import org.noahsark.mqtt.broker.common.factory.MqttModuleFactory;
import org.noahsark.mqtt.broker.protocol.entity.Will;
import org.noahsark.mqtt.broker.repository.ClientSessionRepository;
import org.noahsark.mqtt.broker.repository.WillRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredSession;
import org.noahsark.mqtt.broker.repository.factory.CacheBeanFactory;
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

    /**
     * 本地存放会话信息，定期清除功能待补充 TODO
     */
    private final ConcurrentMap<String, MqttSession> pool = new ConcurrentHashMap<>();

    private CacheBeanFactory beanFactory;
    private ClientSessionRepository sessionRepository;
    private MqttEventBusManager eventBusManager;
    private WillRepository willRepository;
    private MqttEventBus mqttEventBus;

    private static class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    private SessionManager() {
        beanFactory = MqttModuleFactory.getInstance().cacheBeanFactory();

        sessionRepository = beanFactory.clientSessionRepository();
        willRepository = beanFactory.willRepository();
        eventBusManager = MqttModuleFactory.getInstance().mqttEventBusManager();
        mqttEventBus = MqttModuleFactory.getInstance().mqttEventBus();
    }

    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    public void bindToSession(MqttConnection conn, MqttConnectMessage msg, String clientId, String userName) {
        boolean clean = msg.variableHeader().isCleanSession();

        // case 1 session not existing.
        if (!sessionRepository.contain(clientId) || clean) {
            // case 1
            MqttSession newSession = createNewSession(conn, msg, clientId, userName);

            // publish the session
            pool.put(clientId, newSession);

            // 更新缓存
            updateCache(newSession, clientId);

            LOG.info("create new mqtt session:{}", clientId);

            return;
        }

        // case 2 session existing
        StoredSession oldStoredSession = sessionRepository.getSession(clientId);
        boolean isLocal = eventBusManager.getCurrentServer().getId().equals(oldStoredSession.getServerId());

        if (isLocal) {
            rebuildFromLocalSession(oldStoredSession, conn, msg, clientId, userName);
        } else {
            rebuildFromRemoteSession(oldStoredSession, conn, msg, clientId, userName);
        }

    }

    private void rebuildFromLocalSession(StoredSession oldStoredSession, MqttConnection conn, MqttConnectMessage msg,
                                         String clientId, String userName) {
        MqttSession oldSession = pool.get(clientId);

        if (oldSession == null) {
            rebuildFromRemoteSession(oldStoredSession, conn, msg, clientId, userName);
            return;
        }

        if (oldSession.connected()) {
            oldSession.closeImmediately();
            oldSession.disconnect();
        }

        copySessionConfig(msg, oldSession);
        oldSession.bind(conn);
        oldSession.markConnected();

        // 更新缓存
        updateCache(oldSession, clientId);
    }

    private void rebuildFromRemoteSession(StoredSession oldStoredSession, MqttConnection conn, MqttConnectMessage msg,
                                          String clientId, String userName) {

        if (!eventBusManager.getCurrentServer().getId().equals(oldStoredSession.getServerId()) &&
                MqttSession.MqttSessionStatus.valueOf(oldStoredSession.getStatus())
                        .equals(MqttSession.MqttSessionStatus.CONNECTED)) {
            // 发送离线消息到指定的服务器，通知客户端下线
            // TODO
            ClusterClientLogoutInfo clientLogoutInfo = new ClusterClientLogoutInfo();
            clientLogoutInfo.setServerId(oldStoredSession.getServerId());
            clientLogoutInfo.setClientId(clientId);

            ClusterMessage clusterMessage = new ClusterMessage(ClusterMessage.ClusterMessageType.LOGOUT, clientLogoutInfo);
            mqttEventBus.broadcast(clusterMessage);
        }

        MqttSession newSession = createNewSession(conn, msg, clientId, userName);
        // 将会话信息存入本地
        pool.put(clientId, newSession);

        // 恢复消息超时队列
        // TODO
        newSession.rebuildInflightTimoutQueueFromCache();

        // 恢复客户端订阅关系
        // TODO
        newSession.resubscribeFromCache();

        // 更新缓存
        updateCache(newSession, clientId);

    }

    private void updateCache(MqttSession newSession, String clientId) {
        // 会话写入缓存
        StoredSession storedSession = toStoredSession(newSession);
        sessionRepository.updateSession(clientId, storedSession);

        // Will 写入缓存
        Will will = newSession.getWill();
        if (will != null) {
            willRepository.updateWill(clientId, will);
        } else {
            willRepository.removeWill(clientId);
        }
    }

    private StoredSession toStoredSession(MqttSession session) {
        StoredSession storedSession = new StoredSession();

        storedSession.setClientId(session.getClientId());
        storedSession.setUserName(session.getUserName());
        storedSession.setClean(session.isClean());
        storedSession.setStatus(session.getStatus().value());
        storedSession.setServerId(eventBusManager.getCurrentServer().getId());

        return storedSession;
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

    public MqttSession retrieve(String clientId) {
        return pool.get(clientId);
    }

    public void remove(String clientId) {
        pool.remove(clientId);
    }

    public void disconnect(String clientId) {
        logout(clientId, false);
    }

    public void logout(String clientId, boolean needClose) {

        Integer serverId = eventBusManager.getCurrentServer().getId();
        StoredSession storedSession = sessionRepository.getSession(clientId);
        boolean isRelogin = (storedSession != null && !serverId.equals(storedSession.getServerId()));

        MqttSession session = retrieve(clientId);
        if (session == null) {
            LOG.info("Some other thread already removed the session CId={}", clientId);
            return;
        }

        // 1. 断开本地会话
        if (needClose) {
            session.closeImmediately();
        }

        session.disconnect();

        if (!isRelogin) {
            // 2. 清除本地的订阅信息
            session.cleanSubscriptions();

            // 3. 清除 inflight 数据
            session.cleanInflightMsg();

            // 5. 清除will 数据
            willRepository.removeWill(clientId);

            // 6. 清除缓存的会话
            sessionRepository.removeSession(clientId);
        }

        // 6. 清除本地会话
        pool.remove(clientId);
    }

}
