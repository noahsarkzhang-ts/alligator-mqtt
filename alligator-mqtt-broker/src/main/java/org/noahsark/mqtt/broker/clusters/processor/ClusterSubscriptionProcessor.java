package org.noahsark.mqtt.broker.clusters.processor;

import org.noahsark.mqtt.broker.clusters.entity.ClusterMessage;
import org.noahsark.mqtt.broker.clusters.entity.ClusterSubscriptionInfo;
import org.noahsark.mqtt.broker.common.factory.MqttBeanFactory;
import org.noahsark.rpc.common.dispatcher.AbstractProcessor;
import org.noahsark.rpc.common.remote.Response;
import org.noahsark.rpc.common.remote.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群订阅消息处理器
 *
 * @author zhangxt
 * @date 2022/12/21 14:29
 **/
public class ClusterSubscriptionProcessor extends AbstractProcessor<ClusterSubscriptionInfo> {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterSubscriptionProcessor.class);

    @Override
    protected void execute(ClusterSubscriptionInfo clusterSubscriptionInfo, RpcContext rpcContext) {
        LOG.info("Receive cluster subscription info:{}", clusterSubscriptionInfo);

        ClusterMessage clusterMessage = new ClusterMessage(ClusterMessage.ClusterMessageType.SUBSCRIPTION, clusterSubscriptionInfo);
        MqttBeanFactory.getInstance().mqttEventBus().receive(clusterMessage);

        rpcContext.sendResponse(Response.buildCommonResponse(rpcContext.getCommand(), 0, "success"));
    }

    @Override
    protected Class<ClusterSubscriptionInfo> getParamsClass() {
        return ClusterSubscriptionInfo.class;
    }

    @Override
    protected int getBiz() {
        return 5;
    }

    @Override
    protected int getCmd() {
        return 102;
    }
}
