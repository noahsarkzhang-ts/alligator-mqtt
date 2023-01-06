package org.noahsark.mqtt.broker.protocol.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.ReferenceCountUtil;
import org.noahsark.mqtt.broker.transport.session.MqttSession;
import org.noahsark.mqtt.broker.transport.session.SessionManager;
import org.noahsark.mqtt.broker.common.util.NettyUtils;
import org.noahsark.mqtt.broker.transport.session.Context;
import org.noahsark.mqtt.broker.transport.session.MqttConnection;
import org.noahsark.mqtt.broker.protocol.DefaultMqttEngine;
import org.noahsark.mqtt.broker.protocol.MqttEngine;
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
