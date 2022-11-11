package org.noahsrk.mqtt.broker.server.dispatcher;

import io.netty.handler.codec.mqtt.MqttMessageType;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;
import org.noahsrk.mqtt.broker.server.processor.ConnectMessageProcessor;
import org.noahsrk.mqtt.broker.server.processor.DisconnectProcessor;
import org.noahsrk.mqtt.broker.server.processor.MessageProcessor;
import org.noahsrk.mqtt.broker.server.processor.PingReqProcessor;
import org.noahsrk.mqtt.broker.server.processor.PublishProcessor;
import org.noahsrk.mqtt.broker.server.processor.SubscribeProcessor;
import org.noahsrk.mqtt.broker.server.processor.UnsubscribeProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * MQTT 事件发放器
 *
 * @author zhangxt
 * @date 2022/11/01 17:57
 **/
public class Dispatcher {

    private static Map<MqttMessageType, MessageProcessor> processors = new HashMap<>();

    static {
        processors.put(MqttMessageType.CONNECT, new ConnectMessageProcessor());
        processors.put(MqttMessageType.PINGREQ, new PingReqProcessor());
        processors.put(MqttMessageType.DISCONNECT, new DisconnectProcessor());
        processors.put(MqttMessageType.SUBSCRIBE, new SubscribeProcessor());
        processors.put(MqttMessageType.UNSUBSCRIBE, new UnsubscribeProcessor());
        processors.put(MqttMessageType.PUBLISH, new PublishProcessor());

        // TODO 添加消息处理器
    }

    public void put(MqttMessageType type, MessageProcessor processor) {
        processors.put(type, processor);
    }

    public void remove(MqttMessageType type) {
        processors.remove(type);
    }

    public MessageProcessor get(MqttMessageType type) {
        return processors.get(type);
    }

}
