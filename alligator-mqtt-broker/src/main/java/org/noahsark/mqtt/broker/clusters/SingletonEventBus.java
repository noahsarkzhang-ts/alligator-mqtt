package org.noahsark.mqtt.broker.clusters;

import org.noahsark.mqtt.broker.clusters.entity.ClusterMessage;
import org.noahsark.mqtt.broker.common.thread.ServiceThread;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 内存版的消息总线
 *
 * @author zhangxt
 * @date 2022/11/25 14:08
 **/
public class SingletonEventBus extends AbstractMqttEventBus {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonEventBus.class);

    private BlockingQueue<ClusterMessage> messages;

    private MessageProcessThread processThread;

    @Override
    public void init() {
        super.init();

        messages = new LinkedBlockingQueue<>();
        processThread = new MessageProcessThread(messages);
    }

    @Override
    public String alias() {
        return "singleton";
    }

    @Override
    public void broadcast(ClusterMessage msg) {

        messages.offer(msg);
    }

    @Override
    public ClusterMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
        return messages.poll(timeout, unit);
    }

    @Override
    public void startup() {
        super.startup();

        processThread.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();

        processThread.shutdown();
    }

    private class MessageProcessThread extends ServiceThread {

        /**
         * 超时时间
         */
        private static final int TIMEOUT_MS = 60 * 1000;

        private BlockingQueue<ClusterMessage> messages;

        public MessageProcessThread(BlockingQueue<ClusterMessage> messages) {
            this.messages = messages;
        }

        @Override
        public void run() {
            LOG.info("Event Bus Thread start in {}", LocalDateTime.now());

            ClusterMessage message;

            while (!this.isStopped()) {
                try {

                    message = messages.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);

                    if (message == null) {
                        continue;
                    }

                    switch (message.getMessageType()) {
                        case PUBLISH: {
                            publish2Subscribers((PublishInnerMessage) message.getMessage());
                            break;
                        }
                        default: {
                            LOG.info("unsupported operation: {}", message.getMessage());
                            break;
                        }
                    }

                } catch (Exception ex) {
                    LOG.warn("catch an exception!", ex);
                }
            }

            LOG.info(" {} stop in {}", this.getServiceName(), LocalDateTime.now());
        }

        @Override
        public String getServiceName() {
            return "singleton-event-bus-thread";
        }
    }
}
