package org.noahsrk.mqtt.broker.server.protocol;

import java.util.concurrent.TimeUnit;

/**
 * 消息推送及转发器
 *
 * @author zhangxt
 * @date 2022/11/11 15:40
 **/
public interface EventBus {

    boolean emit(PublishedMessage message);

    PublishedMessage poll(long timeout, TimeUnit unit) throws InterruptedException;

    void publish2Subscribers(PublishedMessage message);
}
