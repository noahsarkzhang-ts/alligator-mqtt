package org.noahsrk.mqtt.broker.server.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.util.ReferenceCountUtil;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PubRec 处理器
 *
 * @author zhangxt
 * @date 2022/11/14 15:17
 **/
public class PubRecProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PubRecProcessor.class);

    private SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {
        try {
            final int messageID = ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();
            final MqttSession session = sessionManager.retrieve(context.getConnection().getClientId());

            String clientId = context.getConnection().getClientId();
            LOG.info("Receive PUBREC message. CId={}, messageId: {}", clientId, messageID);
            session.processPubRec(messageID);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
