package org.noahsrk.mqtt.broker.server.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PubComp 处理器
 *
 * @author zhangxt
 * @date 2022/11/14 15:29
 **/
public class PubCompProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PubRecProcessor.class);

    private SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {
        MqttConnection connection = context.getConnection();
        final int messageID = ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();

        String clientId = connection.getClientId();

        final MqttSession session = sessionManager.retrieve(clientId);
        session.processPubComp(messageID);
    }
}
