package org.noahsrk.mqtt.broker.server.processor;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.ReferenceCountUtil;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.noahsrk.mqtt.broker.server.exception.ConnectException;
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
public class ConnectProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectProcessor.class);

    private Authenticator authenticator = new AcceptAllAuthenticator();

    private SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {

        MqttConnectMessage message = (MqttConnectMessage) msg;
        MqttConnection connection = context.getConnection();

        try {

            MqttConnectPayload payload = message.payload();
            String clientId = payload.clientIdentifier();
            String userName = payload.userName();
            byte[] pwd = payload.passwordInBytes();

            LOG.info("Receive connect message. clientId={}", clientId);

            // 1. 校验，包括版本
            validate(connection, message, clientId);

            // 2. 登陆
            login(connection, clientId, userName, pwd);

            // 3. 创建会话
            bindSession(connection, message, clientId, userName);

            // 4. 处理缓存消息
            postMessageHandle(connection, message, clientId);

            // 5. 成功登陆
            connection.sendConnAck(false);
        } catch (ConnectException ex) {
            connection.abortConnection(ex.getCode());
        } finally {
            ReferenceCountUtil.release(message);
        }

    }

    private void validate(MqttConnection connection, MqttConnectMessage msg, String clientId) {

        if (connection.isNotProtocolVersion(msg, MqttVersion.MQTT_3_1) && connection.isNotProtocolVersion(msg, MqttVersion.MQTT_3_1_1)) {
            LOG.warn("MQTT protocol version is not valid. CId={} channel: {}", clientId, connection.getChannel());

            // connection.abortConnection(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
            throw new ConnectException(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
        }

    }

    private void login(MqttConnection conn, String clientId, String userName, byte[] pwd) {

        if (!authenticator.checkValid(clientId, userName, pwd)) {
            LOG.error("Authenticator has rejected the MQTT credentials CId={}, username={}", clientId, userName);

            throw new ConnectException(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
        }

        NettyUtils.clientID(conn.getChannel(), clientId);
        NettyUtils.userName(conn.getChannel(), userName);
    }

    private void bindSession(MqttConnection conn, MqttConnectMessage msg, String clientId, String userName) {
        // TODO
        sessionManager.bindToSession(conn, msg, clientId, userName);
        NettyUtils.sessionId(conn.getChannel(), clientId);
    }

    private void postMessageHandle(MqttConnection connection, MqttConnectMessage msg, String clientId) {
        // TODO
        connection.initializeKeepAliveTimeout(msg, clientId);
        connection.setupInflightResender(connection.getChannel());
    }
}
