package org.noahsark.mqtt.broker.repository.memory;

import org.noahsark.mqtt.broker.repository.OffsetRepository;

import java.util.Map;

/**
 * 内存版 Offset Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 15:55
 **/
public class MemoryOffsetRepository implements OffsetRepository {

    @Override
    public void addTopicOffset(String clientId, String topic, long offset) {

    }

    @Override
    public void updateTopicOffset(String clientId, String topic, long offset) {

    }

    @Override
    public int getTopicOffset(String clientId, String topic) {
        return 0;
    }

    @Override
    public Map<String, Integer> getAllTopicOffsets(String clientId) {
        return null;
    }
}
