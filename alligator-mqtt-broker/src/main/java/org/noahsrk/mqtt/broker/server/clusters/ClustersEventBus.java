package org.noahsrk.mqtt.broker.server.clusters;

import org.noahsrk.mqtt.broker.server.core.MqttEventBus;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;

import java.util.concurrent.TimeUnit;

/**
 * 集群版的 EventBus
 *
 * @author zhangxt
 * @date 2022/12/07 10:02
 **/
public class ClustersEventBus implements MqttEventBus {


    @Override
    public void broadcast(PublishInnerMessage msg) {

    }

    @Override
    public PublishInnerMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public void publish2Subscribers(PublishInnerMessage message) {

    }


}
