package org.noahsark.mqtt.broker.clusters.processor;

import org.noahsark.mqtt.broker.clusters.entity.ClusterMessage;
import org.noahsark.mqtt.broker.clusters.entity.ClusterPublishInnerInfo;
import org.noahsark.mqtt.broker.common.factory.MqttModuleFactory;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.rpc.common.dispatcher.AbstractProcessor;
import org.noahsark.rpc.common.remote.Response;
import org.noahsark.rpc.common.remote.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 广播Publish消息处理器
 *
 * @author zhangxt
 * @date 2022/12/19 11:14
 **/
public class ClusterPublishProcessor extends AbstractProcessor<ClusterPublishInnerInfo> {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterPublishProcessor.class);

    @Override
    protected void execute(ClusterPublishInnerInfo clusterPublishInnerInfo, RpcContext rpcContext) {

        LOG.info("Receive cluster publish info:{}", clusterPublishInnerInfo);

        PublishInnerMessage publishInnerMessage = copyPublishMessage(clusterPublishInnerInfo);

        ClusterMessage clusterMessage = new ClusterMessage(ClusterMessage.ClusterMessageType.PUBLISH, publishInnerMessage);
        MqttModuleFactory.getInstance().mqttEventBus().receive(clusterMessage);

        rpcContext.sendResponse(Response.buildCommonResponse(rpcContext.getCommand(), 0, "success"));

    }

    @Override
    protected Class<ClusterPublishInnerInfo> getParamsClass() {
        return ClusterPublishInnerInfo.class;
    }

    @Override
    protected int getBiz() {
        return 5;
    }

    @Override
    protected int getCmd() {
        return 101;
    }

    private PublishInnerMessage copyPublishMessage(ClusterPublishInnerInfo clusterPublishInnerInfo) {
        PublishInnerMessage message = new PublishInnerMessage();

        message.setMessageId(clusterPublishInnerInfo.getMessageId());
        message.setRetain(clusterPublishInnerInfo.isRetain());
        message.setQos(clusterPublishInnerInfo.getQos());
        message.setTopic(clusterPublishInnerInfo.getTopic());
        message.setPayload(clusterPublishInnerInfo.getPayload());

        return message;
    }
}
