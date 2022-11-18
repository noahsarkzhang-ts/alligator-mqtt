package org.noahsrk.mqtt.broker.server.exception;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

/**
 * 连接异常
 *
 * @author zhangxt
 * @date 2022/11/18 15:48
 **/
public class ConnectException extends RuntimeException {

    private MqttConnectReturnCode code;

    public ConnectException(MqttConnectReturnCode code) {
        this.code = code;
    }

    public MqttConnectReturnCode getCode() {
        return code;
    }
}
