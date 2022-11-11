package org.noahsrk.mqtt.broker.server.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import org.noahsrk.mqtt.broker.server.context.Context;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;

/**
 * 连接断开处理器
 *
 * @author zhangxt
 * @date 2022/11/09 13:53
 **/
public class DisconnectProcessor implements MessageProcessor {

    @Override
    public void handleMessage(Context context, MqttMessage msg) {
        MqttConnection conn = context.getConnection();

        conn.processDisconnect(msg);
    }
}
