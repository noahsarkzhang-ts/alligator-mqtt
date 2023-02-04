package org.noahsark.mqtt.broker.repository.memory;

import org.noahsark.mqtt.broker.repository.ClientSessionRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版Session Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 15:39
 **/
public class MemoryClientSessionRepository implements ClientSessionRepository {

    private final ConcurrentMap<String, StoredSession> sessions = new ConcurrentHashMap<>();

    @Override
    public StoredSession getSession(String clientId) {
        return sessions.get(clientId);
    }

    @Override
    public void addSession(String clientId, StoredSession session) {
        sessions.put(clientId, session);
    }

    @Override
    public void updateSession(String clientId, StoredSession session) {
        sessions.put(clientId, session);
    }

    @Override
    public void removeSession(String clientId) {
        sessions.remove(clientId);
    }

    @Override
    public boolean contain(String clientId) {
        return sessions.containsKey(clientId);
    }


}
