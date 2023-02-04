package org.noahsark.mqtt.broker.repository.entity;

/**
 * 用户持久化类
 *
 * @author zhangxt
 * @date 2023/01/29 15:32
 **/
public class StoredUser {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 状态
     */
    private byte status;

    public StoredUser() {
    }

    public StoredUser(String username, String password, byte status) {
        this.username = username;
        this.password = password;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StoredUser{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", status=" + status +
                '}';
    }
}
