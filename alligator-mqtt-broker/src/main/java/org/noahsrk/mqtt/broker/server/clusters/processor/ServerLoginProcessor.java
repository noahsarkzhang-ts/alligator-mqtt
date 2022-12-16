package org.noahsrk.mqtt.broker.server.clusters.processor;

import org.noahsark.rpc.common.dispatcher.AbstractProcessor;
import org.noahsark.rpc.common.remote.Response;
import org.noahsark.rpc.common.remote.RpcContext;
import org.noahsark.rpc.socket.session.Session;
import org.noahsrk.mqtt.broker.server.clusters.MqttClusterGrid;
import org.noahsrk.mqtt.broker.server.clusters.bean.Loginfo;
import org.noahsrk.mqtt.broker.server.clusters.bean.ServerSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mqtt node 认证处理器
 *
 * @author zhangxt
 * @date 2022/12/13 13:47
 **/
public class ServerLoginProcessor extends AbstractProcessor<Loginfo> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerLoginProcessor.class);

    @Override
    protected void execute(Loginfo loginfo, RpcContext rpcContext) {
        LOG.info("Receive login message:{}", loginfo);

        Session session = (Session) rpcContext.getSession();
        Integer index = loginfo.getClientId();
        session.setStatus(Session.SessionStatus.AUTHORIZED);

        ServerSubject subject = new ServerSubject(index.toString());
        session.setSubject(subject);

        MqttClusterGrid.getInstance().addServerSession(index, session);
        LOG.info("MQTT node[{}] session has built.", index);

        rpcContext.sendResponse(Response.buildCommonResponse(rpcContext.getCommand(), 0, "success"));
    }

    @Override
    protected Class<Loginfo> getParamsClass() {
        return Loginfo.class;
    }

    @Override
    protected int getBiz() {
        return 5;
    }

    @Override
    protected int getCmd() {
        return 100;
    }
}
