package org.noahsrk.mqtt.broker.server.clusters;

import java.util.Objects;

/**
 * Mqtt 节点信息
 *
 * @author zhangxt
 * @date 2022/12/07 10:14
 **/
public class MqttServerInfo {

    private Integer id;

    private String ip;

    private Integer port;

    public MqttServerInfo() {
    }

    public MqttServerInfo(Integer id, String ip, Integer port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MqttServerInfo)) {
            return false;
        }
        MqttServerInfo that = (MqttServerInfo) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "MqttServerInfo{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
