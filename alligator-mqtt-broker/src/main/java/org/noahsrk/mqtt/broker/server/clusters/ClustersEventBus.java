package org.noahsrk.mqtt.broker.server.clusters;

import org.noahsrk.mqtt.broker.server.core.AbstractMqttEventBus;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 集群版的 EventBus
 *
 * @author zhangxt
 * @date 2022/12/07 10:02
 **/
public class ClustersEventBus extends AbstractMqttEventBus {

    private static final Logger LOG = LoggerFactory.getLogger(ClustersEventBus.class);

    private BlockingQueue<PublishInnerMessage> sending;

    private BlockingQueue<PublishInnerMessage> receive;

    public ClustersEventBus() {
        sending = new LinkedBlockingQueue<>();
        receive = new LinkedBlockingQueue<>();
    }

    @Override
    public void broadcast(PublishInnerMessage msg) {
        // TODO
        sending.offer(msg);
    }

    @Override
    public void receive(PublishInnerMessage msg) {
        // TODO
        receive.offer(msg);
        //publish2Subscribers(msg);
    }

}
