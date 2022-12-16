package org.noahsrk.mqtt.broker.server.core;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.noahsrk.mqtt.broker.server.context.MqttSession;
import org.noahsrk.mqtt.broker.server.context.SessionManager;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.noahsrk.mqtt.broker.server.security.PermitAllAuthorizator;
import org.noahsrk.mqtt.broker.server.subscription.Subscription;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 内存版的消息总线
 *
 * @author zhangxt
 * @date 2022/11/25 14:08
 **/
public class MemoryMqttEventBus extends AbstractMqttEventBus {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryMqttEventBus.class);

    private BlockingQueue<PublishInnerMessage> messages;

    private static final class Holder {
        private static final MqttEventBus INSTANCE = new MemoryMqttEventBus();
    }

    private MemoryMqttEventBus() {
        messages = new LinkedBlockingQueue<>();
    }

    public static MqttEventBus getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void broadcast(PublishInnerMessage msg) {
        LOG.info("Receive a PublishedMessage:{},{}", msg.getTopic(), msg.getQos());

        messages.offer(msg);
    }

    @Override
    public PublishInnerMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
        return messages.poll(timeout, unit);
    }



}
