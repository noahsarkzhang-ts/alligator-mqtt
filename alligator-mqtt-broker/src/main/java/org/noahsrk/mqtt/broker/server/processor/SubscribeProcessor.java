package org.noahsrk.mqtt.broker.server.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.noahsrk.mqtt.broker.server.core.DefaultMqttEngine;
import org.noahsrk.mqtt.broker.server.core.MqttEngine;
import org.noahsrk.mqtt.broker.server.protocol.PostOffice;
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

    // private PostOffice postOffice = PostOffice.getInstance();

    private MqttEngine mqttEngine = DefaultMqttEngine.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {
        MqttConnection conn = context.getConnection();
        Channel channel = conn.getChannel();

        final String clientId = NettyUtils.sessionId(channel);

        LOG.info("SUBSCRIBE received , CId={}, channel: {}", clientId, channel);
        if (!conn.isConnected()) {
            LOG.warn("SUBSCRIBE received on already closed connection, CId={}, channel: {}", clientId, channel);
            // dropConnection();
            return;
        }

        MqttSubscribeMessage message = (MqttSubscribeMessage) msg;
        MqttSession mqttSession = SessionManager.getInstance().retrieve(clientId);

        mqttEngine.subcribe(mqttSession,message);

        //postOffice.subscribeClientToTopics(message, clientID, NettyUtils.userName(channel), conn);
    }
}
