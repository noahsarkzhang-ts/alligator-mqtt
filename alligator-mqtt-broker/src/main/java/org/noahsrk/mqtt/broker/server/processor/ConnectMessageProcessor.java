package org.noahsrk.mqtt.broker.server.processor;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.noahsrk.mqtt.broker.server.security.AcceptAllAuthenticator;
import org.noahsrk.mqtt.broker.server.security.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;

/**
 * 连接消息处理器
 *
 * @author zhangxt
 * @date 2022/11/01 17:54
 **/
public class ConnectMessageProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectMessageProcessor.class);

    private Authenticator authenticator = new AcceptAllAuthenticator();

    private SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {

        MqttConnectMessage message = (MqttConnectMessage) msg;
        MqttConnection connection = context.getConnection();
        MqttConnectPayload payload = message.payload();
        String clientId = payload.clientIdentifier();

        LOG.info("Receive connect message. clientId={}", clientId);

        // 1. 校验，包括版本
        if (!validate(connection, message)) {
            return;
        }

        // 2. 登陆
        if (!login(connection, message)) {
            return;
        }
        // 3. 创建会话
        bindSession(connection,message,clientId);

        // 4. 处理缓存消息
        postMessageHandle(connection, message, clientId);

        // 5. 成功登陆
        connection.sendConnAck(false);

    }

    private boolean validate(MqttConnection connection, MqttConnectMessage msg) {

        MqttConnectPayload payload = msg.payload();
        String clientId = payload.clientIdentifier();

        if (connection.isNotProtocolVersion(msg, MqttVersion.MQTT_3_1) && connection.isNotProtocolVersion(msg, MqttVersion.MQTT_3_1_1)) {
            LOG.warn("MQTT protocol version is not valid. CId={} channel: {}", clientId, connection.getChannel());
            connection.abortConnection(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);

            return false;
        }

        return true;
    }

    private boolean login(MqttConnection conn, MqttConnectMessage msg) {

        boolean success = true;

        MqttConnectPayload payload = msg.payload();
        String clientId = payload.clientIdentifier();
        String userName = msg.payload().userName();
        byte[] pwd = msg.payload().passwordInBytes();

        if (!authenticator.checkValid(clientId, userName, pwd)) {
            LOG.error("Authenticator has rejected the MQTT credentials CId={}, username={}", clientId, userName);
            success = false;
        }

        if (!success) {
            conn.abortConnection(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
        }

        NettyUtils.clientID(conn.getChannel(), clientId);
        NettyUtils.userName(conn.getChannel(), userName);

        LOG.info("CId={}, username={} has login: {}", clientId, userName, success ? "successful" : "failed");

        return success;
    }

    private void bindSession(MqttConnection conn, MqttConnectMessage msg, String clientId) {
        // TODO
        sessionManager.bindToSession(conn, msg, clientId);
    }

    private void postMessageHandle(MqttConnection connection, MqttConnectMessage msg, String clientId) {
        // TODO
        connection.initializeKeepAliveTimeout(msg, clientId);
    }
}
