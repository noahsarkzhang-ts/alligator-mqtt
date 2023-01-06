package org.noahsark.mqtt.broker.protocol.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
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

/**
 * 订阅消息
 *
 * @author zhangxt
 * @date 2022/11/11 14:51
 **/
public class SubscribeProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SubscribeProcessor.class);

    private MqttEngine mqttEngine = DefaultMqttEngine.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {

        MqttSubscribeMessage message = (MqttSubscribeMessage) msg;
        MqttConnection conn = context.getConnection();

        try {
            Channel channel = conn.getChannel();
            final String clientId = NettyUtils.sessionId(channel);

            LOG.info("SUBSCRIBE received , CId={}, channel: {}", clientId, channel);
            if (!conn.isConnected()) {
                LOG.warn("SUBSCRIBE received on already closed connection, CId={}, channel: {}", clientId, channel);
                // dropConnection();
                return;
            }

            MqttSession mqttSession = SessionManager.getInstance().retrieve(clientId);

            mqttEngine.subcribe(mqttSession, message);
        } finally {
            ReferenceCountUtil.release(message);
        }

    }
}
