package org.noahsrk.mqtt.broker.server.core;

import org.noahsrk.mqtt.broker.server.clusters.bean.ClusterMessage;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private BlockingQueue<ClusterMessage> messages;

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
    public void broadcast(ClusterMessage msg) {

        messages.offer(msg);
    }

    @Override
    public ClusterMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
        return messages.poll(timeout, unit);
    }



}
