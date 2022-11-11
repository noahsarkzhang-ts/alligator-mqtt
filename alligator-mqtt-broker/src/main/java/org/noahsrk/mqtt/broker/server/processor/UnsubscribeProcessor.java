package org.noahsrk.mqtt.broker.server.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.protocol.PostOffice;
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

    private PostOffice postOffice = PostOffice.getInstance();

    @Override
    public void handleMessage(Context context, MqttMessage msg) {

        MqttConnection conn = context.getConnection();
        Channel channel = conn.getChannel();
        MqttUnsubscribeMessage message = (MqttUnsubscribeMessage)msg;

        List<String> topics = message.payload().topics();
        String clientID = NettyUtils.clientID(channel);

        LOG.trace("Processing UNSUBSCRIBE message. CId={}, topics: {}", clientID, topics);
        postOffice.unsubscribe(topics, conn, message.variableHeader().messageId());
    }
}