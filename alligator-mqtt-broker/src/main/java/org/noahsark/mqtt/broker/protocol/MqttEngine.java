package org.noahsark.mqtt.broker.protocol;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import org.noahsark.mqtt.broker.protocol.entity.Will;
import org.noahsark.mqtt.broker.transport.session.MqttSession;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.protocol.subscription.Subscription;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;

import java.util.List;
import java.util.Set;

/**
 * Mqtt 中枢处理类
 *
 * @author zhangxt
 * @date 2022/11/25 10:27
 **/
public interface MqttEngine {

    void receivedPublishQos0(MqttSession session, PublishInnerMessage msg);

    void receivedPublishQos1(MqttSession session, PublishInnerMessage msg);

    void receivedPublishQos2(MqttSession session, PublishInnerMessage msg);

    void receivePubrel(MqttSession session, PublishInnerMessage msg);

    void subscribe(MqttSession session, MqttSubscribeMessage msg);

    void subscribe(List<Subscription> subscriptions);

    void unsubscribe(MqttSession session, MqttUnsubscribeMessage msg);

    void unsubscribe(List<Subscription> subscriptions);

    Set<Subscription> matchQosSharpening(Topic topic);

    MqttQoS lowerQosToTheSubscriptionDesired(Subscription sub, MqttQoS qos);

    void fireWill(Will will);
}
