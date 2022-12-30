package org.noahsark.mqtt.broker.sever;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Before;
import org.junit.Test;
import org.noahsrk.mqtt.broker.server.core.repository.MemorySubscriptionsRepository;
import org.noahsrk.mqtt.broker.server.core.repository.SubscriptionsRepository;
import org.noahsrk.mqtt.broker.server.subscription.CTrieSubscriptionDirectory;
import org.noahsrk.mqtt.broker.server.subscription.Subscription;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CtrieSubscription 测试
 *
 * @author zhangxt
 * @date 2022/12/21 10:49
 **/
public class CTrieSubscriptionTest {

    private static final Logger LOG = LoggerFactory.getLogger(CTrieSubscriptionTest.class);

    private CTrieSubscriptionDirectory subscriptionsDirectory;

    private SubscriptionsRepository subscriptionsRepository;

    @Before
    public void setUp() {
        subscriptionsRepository = new MemorySubscriptionsRepository();
        subscriptionsDirectory = new CTrieSubscriptionDirectory();
        subscriptionsDirectory.init(subscriptionsRepository);

        String clientId = "device1";
        String topic = "a/b/c";
        MqttQoS qos = MqttQoS.valueOf(2);

        Subscription subscription = new Subscription(clientId,new Topic(topic), qos);
        subscriptionsDirectory.add(subscription);
    }

    @Test
    public void addSubscription() {

        LOG.info("before:{}",subscriptionsDirectory.dumpTree());

        String clientId = "device2";
        String topic = "a/b/c/";
        MqttQoS qos = MqttQoS.valueOf(2);

        Subscription subscription = new Subscription(clientId,new Topic(topic), qos);
        subscriptionsDirectory.add(subscription);

        LOG.info("after:{}",subscriptionsDirectory.dumpTree());
    }

    @Test
    public void removeSubscription() {
        LOG.info("before:{}",subscriptionsDirectory.dumpTree());

        String clientId = "device1";
        String topic = "a/b/c";

        subscriptionsDirectory.removeSubscription(new Topic(topic),clientId);

        LOG.info("after:{}",subscriptionsDirectory.dumpTree());
    }


}
