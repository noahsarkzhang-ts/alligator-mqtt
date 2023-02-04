package org.noahsark.mqtt.broker.repository.memory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.repository.InflightMessageRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内存版 InflightMessageRepository
 *
 * @author zhangxt
 * @date 2023/01/30 10:21
 **/
public class MemoryInflightMessageRepository implements InflightMessageRepository {

    /**
     * 存放发送的 QOS1&2 的 PublishMessage
     */
    private Table<String, Integer, PublishInnerMessage> publishWindow = HashBasedTable.create();

    /**
     * 存放发送的 QOS1&2 的 PubRel 数据
     */
    private Map<String, Set<Integer>> pubrelWindow = new HashMap<>();

    /**
     * 存放收到的 QOS2 的 PublishMessage 数据
     */
    private Table<String, Integer, PublishInnerMessage> qos2Receiving = HashBasedTable.create();

    public MemoryInflightMessageRepository() {
    }

    @Override
    public void addMessage(String clientId, PublishInnerMessage message, boolean isSending) {
        if (isSending) {
            publishWindow.put(clientId, message.getMessageId(), message);
        } else {
            qos2Receiving.put(clientId, message.getMessageId(), message);
        }
    }

    @Override
    public void removeMessage(String clientId, int packetId, boolean isSending) {
        if (isSending) {
            publishWindow.remove(clientId, packetId);
        } else {
            qos2Receiving.remove(clientId, packetId);
        }
    }

    @Override
    public PublishInnerMessage getMessage(String clientId, int packetId, boolean isSending) {
        if (isSending) {
            return publishWindow.get(clientId, packetId);
        } else {
            return qos2Receiving.get(clientId, packetId);
        }
    }

    @Override
    public List<PublishInnerMessage> getAllMessages(String clientId, boolean isSending) {
        if (isSending) {
            return new ArrayList<>(publishWindow.values());
        } else {
            return new ArrayList<>(qos2Receiving.values());
        }
    }

    @Override
    public boolean contain(String clientId, int packetId, boolean isSending) {
        if (isSending) {
            return publishWindow.contains(clientId, packetId);
        } else {
            return qos2Receiving.contains(clientId, packetId);
        }
    }

    @Override
    public void addPubRel(String clientId, int packetId) {

        Set<Integer> window = pubrelWindow.get(clientId);
        if (window != null) {
            window.add(packetId);
        } else {
            window = new HashSet<>();
            window.add(packetId);

            pubrelWindow.put(clientId, window);
        }

    }

    @Override
    public void removePubRel(String clientId, int packetId) {
        Set<Integer> window = pubrelWindow.get(clientId);
        if (window != null) {
            window.remove(packetId);
        }
    }

    @Override
    public Set<Integer> getAllPubRel(String clientId) {
        Set<Integer> window = pubrelWindow.get(clientId);

        if (window != null) {
            return window;
        } else {
            return new HashSet<>();
        }

    }

    @Override
    public void clean(String clientId) {
    }
}
