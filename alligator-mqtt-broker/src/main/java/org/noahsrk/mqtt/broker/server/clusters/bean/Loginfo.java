package org.noahsrk.mqtt.broker.server.clusters.bean;

import java.util.Objects;

/**
 * 客户端登陆信息
 *
 * @author zhangxt
 * @date 2022/12/13 11:52
 **/
public class Loginfo {

    private Integer clientId;

    private String token;

    public Loginfo() {
    }

    public Loginfo(Integer clientId, String token) {
        this.clientId = clientId;
        this.token = token;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
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
        if (!(o instanceof Loginfo)) {
            return false;
        }
        Loginfo loginfo = (Loginfo) o;
        return Objects.equals(getClientId(), loginfo.getClientId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClientId());
    }

    @Override
    public String toString() {
        return "Loginfo{" +
                "clientId=" + clientId +
                ", token='" + token + '\'' +
                '}';
    }
}
