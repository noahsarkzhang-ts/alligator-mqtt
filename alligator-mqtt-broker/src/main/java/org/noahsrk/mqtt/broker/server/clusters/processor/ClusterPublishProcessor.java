package org.noahsrk.mqtt.broker.server.clusters.processor;

import org.noahsark.rpc.common.dispatcher.AbstractProcessor;
import org.noahsark.rpc.common.remote.Response;
import org.noahsark.rpc.common.remote.RpcContext;
import org.noahsrk.mqtt.broker.server.clusters.ClustersEventBus;
import org.noahsrk.mqtt.broker.server.clusters.bean.ClusterMessage;
import org.noahsrk.mqtt.broker.server.clusters.bean.ClusterPublishInnerInfo;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
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
        ClustersEventBus.getInstance().receive(clusterMessage);

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
        message.setTopic(new Topic(clusterPublishInnerInfo.getTopic()));
        message.setPayload(clusterPublishInnerInfo.getPayload());

        return message;
    }
}
