package org.noahsrk.mqtt.broker.server.context;

/**
 * 上下文环境
 *
 * @author zhangxt
 * @date 2022/10/26 10:44
 **/
public class Context {

    private MqttConnection connection;

    public Context(MqttConnection connection) {
        this.connection = connection;
    }

    public MqttConnection getConnection() {
        return connection;
    }

    public void setConnection(MqttConnection connection) {
        this.connection = connection;
    }
}
