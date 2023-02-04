package org.noahsark.mqtt.broker.repository.memory;

import org.noahsark.mqtt.broker.protocol.entity.Will;
import org.noahsark.mqtt.broker.repository.WillRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * 内存版的 Will Repository
 *
 * @author zhangxt
 * @date 2023/01/30 14:12
 **/
public class MemoryWillRepository implements WillRepository {

    private Map<String, Will> wills = new HashMap<>();

    @Override
    public Will getWill(String clientId) {
        return wills.get(clientId);
    }

    @Override
    public void addWill(String clientId, Will will) {
        wills.put(clientId, will);
    }

    @Override
    public void updateWill(String clientId, Will will) {
        wills.put(clientId, will);
    }

    @Override
    public void removeWill(String clientId) {
        wills.remove(clientId);
    }
}
