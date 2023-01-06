package org.noahsark.mqtt.broker.protocol.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import org.noahsark.mqtt.broker.transport.session.Context;

/**
 * 消息处理接口
 *
 * @author zhangxt
 * @date 2022/11/01 17:51
 **/
public interface MessageProcessor {

    void handleMessage(Context context, MqttMessage msg);

}
