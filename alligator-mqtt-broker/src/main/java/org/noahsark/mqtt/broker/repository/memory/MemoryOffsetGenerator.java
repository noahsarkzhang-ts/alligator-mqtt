package org.noahsark.mqtt.broker.repository.memory;

import org.noahsark.mqtt.broker.repository.OffsetGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * 内存版 Offset 生成器
 *
 * @author zhangxt
 * @date 2023/01/31 15:53
 **/
public class MemoryOffsetGenerator implements OffsetGenerator {

    private Map<String, Long> offsets = new HashMap<>();

    @Override
    public long incrOffset(String topic) {

        Long currentOffset = offsets.get(topic);
        Long nextOffset;

        if (currentOffset == null) {
            currentOffset = 0L;
        }

        nextOffset = currentOffset + 1;
        offsets.put(topic, nextOffset);

        return nextOffset.longValue();
    }

    @Override
    public void resetOffset(String topic, long offset) {
        offsets.put(topic, offset);
    }
}
