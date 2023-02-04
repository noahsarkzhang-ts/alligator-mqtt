package org.noahsark.mqtt.broker.repository.memory;

import org.noahsark.mqtt.broker.protocol.entity.RetainedMessage;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;
import org.noahsark.mqtt.broker.repository.RetainedRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版RetainedRepository
 *
 * @author zhangxt
 * @date 2022/12/01 10:53
 **/
public class MemoryRetainedRepository implements RetainedRepository {

    private ConcurrentMap<String, List<RetainedMessage>> pool = new ConcurrentHashMap<>();

    public MemoryRetainedRepository() {
    }

    @Override
    public void clean(String topic) {
        pool.remove(topic);
    }

    @Override
    public void addRetainMessage(String topic, RetainedMessage msg) {

        List<RetainedMessage> msgs = pool.get(topic);

        if (msgs != null) {
            msgs.add(msg);
        } else {
            msgs = new ArrayList<>();
            msgs.add(msg);

            pool.put(topic, msgs);
        }

    }

    @Override
    public List<RetainedMessage> getAllRetainMessage(String topic) {
        List<RetainedMessage> msgs = pool.get(topic);

        if (msgs != null) {
            return msgs;
        } else {
            return new ArrayList<>();
        }
    }
}
