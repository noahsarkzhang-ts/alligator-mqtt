package org.noahsark.mqtt.broker.clusters.processor;

import org.noahsark.mqtt.broker.clusters.entity.ServerLoginfo;
import org.noahsark.mqtt.broker.clusters.entity.ServerSubject;
import org.noahsark.mqtt.broker.common.factory.MqttModuleFactory;
import org.noahsark.rpc.common.dispatcher.AbstractProcessor;
import org.noahsark.rpc.common.remote.Response;
import org.noahsark.rpc.common.remote.RpcContext;
import org.noahsark.rpc.socket.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mqtt node 认证处理器
 *
 * @author zhangxt
 * @date 2022/12/13 13:47
 **/
public class ServerLoginProcessor extends AbstractProcessor<ServerLoginfo> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerLoginProcessor.class);

    @Override
    protected void execute(ServerLoginfo serverLoginfo, RpcContext rpcContext) {
        LOG.info("Receive Server login message:{}", serverLoginfo);

        Session session = (Session) rpcContext.getSession();
        Integer index = serverLoginfo.getServerId();
        session.setStatus(Session.SessionStatus.AUTHORIZED);

        ServerSubject subject = new ServerSubject(index.toString());
        session.setSubject(subject);

        MqttModuleFactory.getInstance().mqttEventBusManager().addServerSession(index, session);
        LOG.info("MQTT node[{}] session has built.", index);

        rpcContext.sendResponse(Response.buildCommonResponse(rpcContext.getCommand(), 0, "success"));
    }

    @Override
    protected Class<ServerLoginfo> getParamsClass() {
        return ServerLoginfo.class;
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
