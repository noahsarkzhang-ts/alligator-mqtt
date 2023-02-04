package org.noahsark.mqtt.broker.repository.entity;

/**
 * 会话持久化类
 *
 * @author zhangxt
 * @date 2023/01/29 15:42
 **/
public class StoredSession {

    /**
     * 会话所属的客户端 id
     */
    private String clientId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 会话是否保持
     */
    private boolean clean;

    /**
     * 所在的 Broker 服务器 id
     */
    private int serverId;

    /**
     * 会话状态
     */
    private int status;

    /**
     * 创建时间
     */
    private long timestamp;

    public StoredSession() {
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "StoredSession{" +
                "clientId='" + clientId + '\'' +
                ", userName='" + userName + '\'' +
                ", clean=" + clean +
                ", serverId=" + serverId +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}
