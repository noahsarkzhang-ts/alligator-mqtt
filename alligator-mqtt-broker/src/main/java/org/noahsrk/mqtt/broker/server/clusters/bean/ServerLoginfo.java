package org.noahsrk.mqtt.broker.server.clusters.bean;

import java.util.Objects;

/**
 * 客户端登陆信息
 *
 * @author zhangxt
 * @date 2022/12/13 11:52
 **/
public class ServerLoginfo {

    private Integer serverId;

    private String token;

    public ServerLoginfo() {
    }

    public ServerLoginfo(Integer serverId, String token) {
        this.serverId = serverId;
        this.token = token;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerLoginfo)) {
            return false;
        }
        ServerLoginfo serverLoginfo = (ServerLoginfo) o;
        return Objects.equals(getServerId(), serverLoginfo.getServerId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerId());
    }

    @Override
    public String toString() {
        return "ServerLoginfo{" +
                "serverId=" + serverId +
                ", token='" + token + '\'' +
                '}';
    }
}
