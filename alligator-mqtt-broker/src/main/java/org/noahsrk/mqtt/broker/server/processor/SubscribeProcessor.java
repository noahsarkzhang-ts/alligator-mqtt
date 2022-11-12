package org.noahsrk.mqtt.broker.server.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
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

    private PostOffice postOffice = PostOffice.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {
        MqttConnection conn = context.getConnection();
        Channel channel = conn.getChannel();
        MqttSubscribeMessage message = (MqttSubscribeMessage) msg;

        final String clientID = NettyUtils.clientID(channel);

        LOG.info("SUBSCRIBE received , CId={}, channel: {}", clientID, channel);

        if (!conn.isConnected()) {
            LOG.warn("SUBSCRIBE received on already closed connection, CId={}, channel: {}", clientID, channel);
            // dropConnection();
            return;
        }

        postOffice.subscribeClientToTopics(message, clientID, NettyUtils.userName(channel), conn);
    }
}
