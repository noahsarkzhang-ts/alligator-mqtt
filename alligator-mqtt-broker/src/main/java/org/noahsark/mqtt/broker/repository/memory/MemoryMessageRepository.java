package org.noahsark.mqtt.broker.repository.memory;

import org.noahsark.mqtt.broker.repository.MessageRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredMessage;

import java.util.List;

/**
 * 内存版 Message Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 15:52
 **/
public class MemoryMessageRepository implements MessageRepository {


    @Override
    public void addMessage(StoredMessage msg) {

    }

    @Override
    public StoredMessage getMessage(String topic, long offset) {
        return null;
    }


    @Override
    public List<StoredMessage> getAllMessage(String topic, long startOffset) {
        return null;
    }
}
