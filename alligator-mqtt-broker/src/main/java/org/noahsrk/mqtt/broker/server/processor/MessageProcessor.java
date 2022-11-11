package org.noahsrk.mqtt.broker.server.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import org.noahsrk.mqtt.broker.server.context.Context;

/**
 * 消息处理接口
 *
 * @author zhangxt
 * @date 2022/11/01 17:51
 **/
public interface MessageProcessor {

    void handleMessage(Context context, MqttMessage msg);

}
