package org.noahsark.mqtt.broker.sever;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Before;
import org.junit.Test;
import org.noahsark.mqtt.broker.repository.memory.MemorySubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.protocol.subscription.CTrieSubscriptionDirectory;
import org.noahsark.mqtt.broker.protocol.subscription.Subscription;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

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

    }

    @Test
    public void addSubscription() {

        LOG.info("before:{}", subscriptionsDirectory.dumpTree());

        String clientId = "device1";
        String topic = "a/";
        MqttQoS qos = MqttQoS.valueOf(2);

        Subscription subscription = new Subscription(clientId, new Topic(topic), qos);
        subscriptionsDirectory.add(subscription);


        clientId = "device2";
        topic = "a/b";
        qos = MqttQoS.valueOf(2);

        subscription = new Subscription(clientId, new Topic(topic), qos);
        subscriptionsDirectory.add(subscription);

        clientId = "device3";
        topic = "a/b/";
        qos = MqttQoS.valueOf(2);

        subscription = new Subscription(clientId, new Topic(topic), qos);
        subscriptionsDirectory.add(subscription);

        LOG.info("after:{}", subscriptionsDirectory.dumpTree());
    }

    @Test
    public void removeSubscription() {
        LOG.info("before:{}", subscriptionsDirectory.dumpTree());

        String clientId = "device1";
        String topic = "a/b/c";

        subscriptionsDirectory.removeSubscription(new Topic(topic), clientId);

        LOG.info("after:{}", subscriptionsDirectory.dumpTree());
    }

    @Test
    public void matchMultiTest() {
        String clientId = "device1";
        String topic = "a/#";
        MqttQoS qos = MqttQoS.valueOf(2);

        Subscription subscription = new Subscription(clientId, new Topic(topic), qos);
        subscriptionsDirectory.add(subscription);

        final Set<Subscription> subscriptions = subscriptionsDirectory.matchQosSharpening(new Topic("a/"));

        subscriptions.forEach(subscription1 -> LOG.info("subscription:{}", subscription1));
    }

    @Test
    public void matchSingleTest() {
        String clientId = "device1";
        String topic = "a/+";
        MqttQoS qos = MqttQoS.valueOf(2);

        Subscription subscription = new Subscription(clientId, new Topic(topic), qos);
        subscriptionsDirectory.add(subscription);

        final Set<Subscription> subscriptions = subscriptionsDirectory.matchQosSharpening(new Topic("a/"));

        subscriptions.forEach(subscription1 -> LOG.info("subscription:{}", subscription1));
    }


}
