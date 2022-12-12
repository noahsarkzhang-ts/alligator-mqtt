package org.noahsrk.mqtt.broker.server.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
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
