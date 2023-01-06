package org.noahsark.mqtt.broker.transport;

import io.netty.handler.codec.mqtt.MqttMessageType;
import org.noahsark.mqtt.broker.protocol.processor.ConnectProcessor;
import org.noahsark.mqtt.broker.protocol.processor.DisconnectProcessor;
import org.noahsark.mqtt.broker.protocol.processor.PingReqProcessor;
import org.noahsark.mqtt.broker.protocol.processor.PubAckProcessor;
import org.noahsark.mqtt.broker.protocol.processor.PubCompProcessor;
import org.noahsark.mqtt.broker.protocol.processor.PubRecProcessor;
import org.noahsark.mqtt.broker.protocol.processor.PubRelProcessor;
import org.noahsark.mqtt.broker.protocol.processor.PublishProcessor;
import org.noahsark.mqtt.broker.protocol.processor.SubscribeProcessor;
import org.noahsark.mqtt.broker.protocol.processor.UnsubscribeProcessor;
import org.noahsark.mqtt.broker.protocol.processor.MessageProcessor;

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
        processors.put(MqttMessageType.CONNECT, new ConnectProcessor());
        processors.put(MqttMessageType.PINGREQ, new PingReqProcessor());
        processors.put(MqttMessageType.DISCONNECT, new DisconnectProcessor());
        processors.put(MqttMessageType.SUBSCRIBE, new SubscribeProcessor());
        processors.put(MqttMessageType.UNSUBSCRIBE, new UnsubscribeProcessor());
        processors.put(MqttMessageType.PUBLISH, new PublishProcessor());
        processors.put(MqttMessageType.PUBACK, new PubAckProcessor());
        processors.put(MqttMessageType.PUBREC, new PubRecProcessor());
        processors.put(MqttMessageType.PUBREL, new PubRelProcessor());
        processors.put(MqttMessageType.PUBCOMP, new PubCompProcessor());

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
