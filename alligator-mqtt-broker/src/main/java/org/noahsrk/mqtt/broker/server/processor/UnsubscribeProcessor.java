package org.noahsrk.mqtt.broker.server.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.ReferenceCountUtil;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.noahsrk.mqtt.broker.server.core.DefaultMqttEngine;
import org.noahsrk.mqtt.broker.server.core.MqttEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 取消订阅消息
 *
 * @author zhangxt
 * @date 2022/11/11 14:52
 **/
public class UnsubscribeProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(UnsubscribeProcessor.class);

    private MqttEngine mqttEngine = DefaultMqttEngine.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {

        MqttUnsubscribeMessage message = (MqttUnsubscribeMessage) msg;
        MqttConnection conn = context.getConnection();

        try {
            Channel channel = conn.getChannel();
            List<String> topics = message.payload().topics();
            final String clientId = NettyUtils.sessionId(channel);

            LOG.trace("Processing UNSUBSCRIBE message. CId={}, topics: {}", clientId, topics);
            MqttSession mqttSession = SessionManager.getInstance().retrieve(clientId);

            mqttEngine.unsubscribe(mqttSession, message);
        } finally {
            ReferenceCountUtil.release(message);
        }
    }
}
