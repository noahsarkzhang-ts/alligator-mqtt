package org.noahsark.mqtt.broker.clusters.entity;

/**
 * 客户端下线消息
 *
 * @author zhangxt
 * @date 2023/02/02 14:08
 **/
public class ClusterClientLogoutInfo {

    private Integer serverId;

    private String clientId;

    public ClusterClientLogoutInfo() {
    }

    public ClusterClientLogoutInfo(Integer serverId, String clientId) {
        this.serverId = serverId;
        this.clientId = clientId;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        return "ClusterClientLogoutInfo{" +
                "serverId=" + serverId +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}
