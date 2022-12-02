package org.noahsrk.mqtt.broker.server.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.noahsrk.mqtt.broker.server.subscription.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版 RetainedRepository
 *
 * @author zhangxt
 * @date 2022/11/14 18:02
 **/
public class MemoryV1RetainedRepository implements RetainedRepository {

    private final ConcurrentMap<Topic, RetainedMessageV1> pool = new ConcurrentHashMap<>();

    private static final class Holder {
        private static final MemoryV1RetainedRepository INSTANCE = new MemoryV1RetainedRepository();
    }

    private MemoryV1RetainedRepository() {

    }

    public static MemoryV1RetainedRepository getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void cleanRetained(Topic topic) {
        pool.remove(topic);
    }

    @Override
    public void retain(Topic topic, MqttPublishMessage msg) {
        final ByteBuf payload = msg.content();
        byte[] rawPayload = new byte[payload.readableBytes()];
        payload.getBytes(0, rawPayload);
        RetainedMessageV1 toStore = new RetainedMessageV1(msg.fixedHeader().qosLevel(), rawPayload);

        pool.put(topic, toStore);
    }

    @Override
    public boolean isEmpty() {
        return pool.isEmpty();
    }

    @Override
    public List<RetainedMessageV1> retainedOnTopic(String topic) {
        List<RetainedMessageV1> list = new ArrayList<>();

        /*RetainedMessageV1 msg = pool.get(new Topic(topic));

        if (msg != null) {
            list.add(msg);
        }*/

        return list;
    }
}
