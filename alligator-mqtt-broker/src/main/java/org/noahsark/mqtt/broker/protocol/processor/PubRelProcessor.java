package org.noahsark.mqtt.broker.protocol.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.util.ReferenceCountUtil;
import org.noahsark.mqtt.broker.transport.session.MqttConnection;
import org.noahsark.mqtt.broker.transport.session.MqttSession;
import org.noahsark.mqtt.broker.transport.session.SessionManager;
import org.noahsark.mqtt.broker.transport.session.Context;
import org.noahsark.mqtt.broker.protocol.DefaultMqttEngine;
import org.noahsark.mqtt.broker.protocol.MqttEngine;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PubRel 处理器
 *
 * @author zhangxt
 * @date 2022/11/14 15:22
 **/
public class PubRelProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PubRecProcessor.class);

    private MqttEngine mqttEngine = DefaultMqttEngine.getInstance();

    private SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {

        try {
            MqttConnection connection = context.getConnection();
            String clientId = connection.getClientId();
            final MqttSession session = sessionManager.retrieve(clientId);
            final int messageID = ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();

            PublishInnerMessage message = session.retrieveMsgQos2(messageID);
            mqttEngine.receivePubrel(session, message);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
