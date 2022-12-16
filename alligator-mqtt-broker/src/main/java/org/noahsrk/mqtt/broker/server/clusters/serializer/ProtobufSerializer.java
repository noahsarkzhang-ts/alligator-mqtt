package org.noahsrk.mqtt.broker.server.clusters.serializer;

import org.noahsark.rpc.common.serializer.Serializer;

/**
 * Protobuf 序列化器
 *
 * @author zhangxt
 * @date 2022/12/13 11:56
 **/
public class ProtobufSerializer implements Serializer {

    @Override
    public byte[] encode(Object object) {
        return ProtostuffUtils.serialize(object);
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> classz) {
        return ProtostuffUtils.deserialize(bytes, classz);
    }
}
