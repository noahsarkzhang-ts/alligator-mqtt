package org.noahsrk.mqtt.broker.server.core.repository;

import org.noahsrk.mqtt.broker.server.core.bean.RetainedMessage;
import org.noahsrk.mqtt.broker.server.subscription.Topic;

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

    private final ConcurrentMap<Topic, RetainedMessage> pool = new ConcurrentHashMap<>();

    private static final class Holder {
        private static final RetainedRepository INSTANCE = new MemoryRetainedRepository();
    }

    private MemoryRetainedRepository() {

    }

    public static RetainedRepository getInstance() {
        return MemoryRetainedRepository.Holder.INSTANCE;
    }

    @Override
    public void cleanRetained(Topic topic) {
        pool.remove(topic);
    }

    @Override
    public void retain(Topic topic, RetainedMessage msg) {
        pool.put(topic, msg);
    }

    @Override
    public boolean isEmpty() {
        return pool.isEmpty();
    }

    @Override
    public List<RetainedMessage> retainedOnTopic(String topic) {
        List<RetainedMessage> list = new ArrayList<>();

        RetainedMessage msg = pool.get(new Topic(topic));

        if (msg != null) {
            list.add(msg);
        }

        return list;
    }
}
