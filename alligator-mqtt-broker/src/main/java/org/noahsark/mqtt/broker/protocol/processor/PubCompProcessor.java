package org.noahsark.mqtt.broker.protocol.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.util.ReferenceCountUtil;
import org.noahsark.mqtt.broker.transport.session.MqttConnection;
import org.noahsark.mqtt.broker.transport.session.MqttSession;
import org.noahsark.mqtt.broker.transport.session.SessionManager;
import org.noahsark.mqtt.broker.transport.session.Context;
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
        try {
            MqttConnection connection = context.getConnection();
            final int messageID = ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();

            String clientId = connection.getClientId();

            final MqttSession session = sessionManager.retrieve(clientId);
            session.processPubComp(messageID);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
