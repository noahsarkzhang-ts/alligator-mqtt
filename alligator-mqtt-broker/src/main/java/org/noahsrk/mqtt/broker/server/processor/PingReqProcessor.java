package org.noahsrk.mqtt.broker.server.processor;

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import org.noahsrk.mqtt.broker.server.context.Context;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

/**
 * 处理 Ping 请求
 *
 * @author zhangxt
 * @date 2022/11/09 11:48
 **/
public class PingReqProcessor implements MessageProcessor {

    @Override
    public void handleMessage(Context context, MqttMessage msg) {

        MqttFixedHeader pingHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, AT_MOST_ONCE,
                false, 0);
        MqttMessage pingResp = new MqttMessage(pingHeader);
        context.getConnection().getChannel().writeAndFlush(pingResp).addListener(CLOSE_ON_FAILURE);
    }
}
