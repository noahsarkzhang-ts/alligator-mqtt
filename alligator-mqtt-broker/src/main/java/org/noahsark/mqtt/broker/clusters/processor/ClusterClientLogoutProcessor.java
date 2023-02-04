package org.noahsark.mqtt.broker.clusters.processor;

import org.noahsark.mqtt.broker.clusters.entity.ClusterClientLogoutInfo;
import org.noahsark.mqtt.broker.transport.session.SessionManager;
import org.noahsark.rpc.common.dispatcher.AbstractProcessor;
import org.noahsark.rpc.common.remote.Response;
import org.noahsark.rpc.common.remote.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端下线处理器
 *
 * @author zhangxt
 * @date 2023/02/02 14:42
 **/
public class ClusterClientLogoutProcessor extends AbstractProcessor<ClusterClientLogoutInfo> {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterClientLogoutProcessor.class);

    @Override
    protected void execute(ClusterClientLogoutInfo clusterClientLogoutInfo, RpcContext rpcContext) {
        LOG.info("Receive cluster logout info:{}", clusterClientLogoutInfo);

        SessionManager.getInstance().logout(clusterClientLogoutInfo.getClientId(), true);

        rpcContext.sendResponse(Response.buildCommonResponse(rpcContext.getCommand(), 0, "success"));
    }

    @Override
    protected Class<ClusterClientLogoutInfo> getParamsClass() {
        return ClusterClientLogoutInfo.class;
    }

    @Override
    protected int getBiz() {
        return 5;
    }

    @Override
    protected int getCmd() {
        return 103;
    }


}
