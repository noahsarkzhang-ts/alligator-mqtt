package org.noahsrk.mqtt.broker.server.clusters.bean;

/**
 * 集群消息
 *
 * @author zhangxt
 * @date 2022/12/21 11:39
 **/
public class ClusterMessage {

    private ClusterMessageType messageType;

    private Object message;

    public ClusterMessage() {
    }

    public ClusterMessage(ClusterMessageType messageType, Object message) {
        this.messageType = messageType;
        this.message = message;
    }

    public ClusterMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(ClusterMessageType messageType) {
        this.messageType = messageType;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public enum ClusterMessageType {
        PUBLISH,SUBSCRIPTION
    }
}
