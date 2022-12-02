package org.noahsrk.mqtt.broker.server.thread;

import org.noahsrk.mqtt.broker.server.core.MemoryMqttEventBus;
import org.noahsrk.mqtt.broker.server.core.MqttEventBus;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
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

    private MqttEventBus eventBus = MemoryMqttEventBus.getInstance();

    @Override
    public void run() {
        log.info("Event Bus Thread start in {}", LocalDateTime.now());

        PublishInnerMessage message;

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
