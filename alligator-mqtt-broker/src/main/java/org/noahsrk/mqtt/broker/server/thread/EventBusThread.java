package org.noahsrk.mqtt.broker.server.thread;

import org.noahsrk.mqtt.broker.server.protocol.EventBus;
import org.noahsrk.mqtt.broker.server.protocol.MemoryEventBus;
import org.noahsrk.mqtt.broker.server.protocol.PublishedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 通用任务处理线程
 *
 * @author zhangxt
 * @date 2022/10/18 09:55
 **/
public class EventBusThread extends ServiceThread {

    private static Logger log = LoggerFactory.getLogger(EventBusThread.class);

    /**
     * 超时时间
     */
    private static final int TIMEOUT_MS = 60 * 1000;

    private EventBus eventBus = MemoryEventBus.getInstance();

    @Override
    public void run() {
        log.info("Event Bus Thread start in {}", LocalDateTime.now());

        PublishedMessage message;

        while (!this.isStopped()) {
            try {

                message = eventBus.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);

                if (message != null) {
                    eventBus.publish2Subscribers(message);
                }

            } catch (Exception ex) {
                log.warn("catch an exception!", ex);
            }
        }

        log.info(" {} stop in {}", this.getServiceName(), LocalDateTime.now());
    }

    @Override
    public String getServiceName() {
        return "event-bus-thread";
    }
}
