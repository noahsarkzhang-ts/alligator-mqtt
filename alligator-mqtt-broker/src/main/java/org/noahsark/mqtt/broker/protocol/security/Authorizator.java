package org.noahsark.mqtt.broker.protocol.security;

import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;

import java.util.List;

/**
 * Topic 授权类
 *
 * @author zhangxt
 * @date 2022/11/25 14:15
 **/
public interface Authorizator {

    List<MqttTopicSubscription> verifyTopicsReadAccess(String clientID, String username, MqttSubscribeMessage msg);

    boolean canWrite(Topic topic, String user, String client);

    boolean canRead(Topic topic, String user, String client);
}
