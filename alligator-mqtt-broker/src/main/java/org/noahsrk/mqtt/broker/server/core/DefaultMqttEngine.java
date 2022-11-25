package org.noahsrk.mqtt.broker.server.core;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.core.repository.MemorySubscriptionsRepository;
import org.noahsrk.mqtt.broker.server.core.repository.MessageRepository;
import org.noahsrk.mqtt.broker.server.core.repository.MysqlMessageRepository;
import org.noahsrk.mqtt.broker.server.core.repository.SubscriptionsRepository;
import org.noahsrk.mqtt.broker.server.security.Authorizator;
import org.noahsrk.mqtt.broker.server.security.PermitAllAuthorizator;
import org.noahsrk.mqtt.broker.server.subscription.CTrieSubscriptionDirectory;
import org.noahsrk.mqtt.broker.server.subscription.SubscriptionsDirectory;

/**
 * 默认的MqttEngine
 *
 * @author zhangxt
 * @date 2022/11/25 14:05
 **/
public class DefaultMqttEngine implements MqttEngine {

    private Authorizator authorizator;

    private MqttEventBus eventBus;

    private MessageRepository messageRepository;

    private SubscriptionsRepository subscriptionsRepository;

    private SubscriptionsDirectory subscriptionsDirectory;

    public void load() {
        authorizator = PermitAllAuthorizator.getInstance();
        eventBus = new MemoryMqttEventBus();
        messageRepository = new MysqlMessageRepository();
        subscriptionsRepository = new MemorySubscriptionsRepository();
        subscriptionsDirectory = new CTrieSubscriptionDirectory();
    }

    @Override
    public void receivedPublishQos0(MqttSession session, MqttPublishMessage msg) {

    }

    @Override
    public void receivedPublishQos1(MqttSession session, MqttPublishMessage msg) {

    }

    @Override
    public void receivedPublishQos2(MqttSession session, MqttPublishMessage msg) {

    }

    @Override
    public void subcribe(MqttSession session, MqttSubscribeMessage msg) {

    }

    @Override
    public void unsubcribe(MqttSession session, MqttUnsubscribeMessage msg) {

    }
}
