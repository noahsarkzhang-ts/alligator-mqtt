package org.noahsrk.mqtt.broker.server.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PubAck 处理逻辑
 *
 * @author zhangxt
 * @date 2022/11/14 10:34
 **/
public class PubAckProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PubAckProcessor.class);

    private SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {

        final int messageID = ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();
        String clientId = context.getConnection().getClientId();

        LOG.info("Receive PUBACK message. CId={}, messageId: {}", clientId, messageID);

        MqttSession session = sessionManager.retrieve(clientId);
        session.pubAckReceived(messageID);
    }
}
