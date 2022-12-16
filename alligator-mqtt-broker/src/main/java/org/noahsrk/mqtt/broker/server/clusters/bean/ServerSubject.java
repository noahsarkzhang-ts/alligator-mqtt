package org.noahsrk.mqtt.broker.server.clusters.bean;

import org.noahsark.rpc.common.remote.Subject;

/**
 * 服务器登陆会话信息
 *
 * @author zhangxt
 * @date 2022/12/15 09:44
 **/
public class ServerSubject implements Subject {

    private String id;

    public ServerSubject(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }
}
