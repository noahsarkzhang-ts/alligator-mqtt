package org.noahsark.mqtt.broker.clusters.entity;

import java.util.Set;

/**
 * 订阅广播消息
 *
 * @author zhangxt
 * @date 2022/12/21 11:34
 **/
public class ClusterSubscriptionInfo {

    // 服务器编号
    private int serverId;

    private Set<String> addition;

    private Set<String> remove;

    public ClusterSubscriptionInfo() {
    }

    public ClusterSubscriptionInfo(int serverId, Set<String> addition, Set<String> remove) {
        this.serverId = serverId;
        this.addition = addition;
        this.remove = remove;
    }

    public boolean changed() {
        return !addition.isEmpty() || !remove.isEmpty();
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public Set<String> getAddition() {
        return addition;
    }

    public void setAddition(Set<String> addition) {
        this.addition = addition;
    }

    public Set<String> getRemove() {
        return remove;
    }

    public void setRemove(Set<String> remove) {
        this.remove = remove;
    }

    @Override
    public String toString() {
        return "ClusterSubscriptionInfo{" +
                "serverId=" + serverId +
                ", addition=" + addition +
                ", remove=" + remove +
                '}';
    }
}
