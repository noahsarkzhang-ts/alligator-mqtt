package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.repository.ClientSessionRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredSession;

/**
 * Redis 版 Session Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 16:00
 **/
public class RedisClientSessionRepository implements ClientSessionRepository {

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    public RedisClientSessionRepository(RedisCmdRunner cmdRunner, RedisScriptRunner scriptRunner) {
        this.cmdRunner = cmdRunner;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public StoredSession getSession(String clientId) {
        return cmdRunner.getSession(clientId);
    }

    @Override
    public void addSession(String clientId, StoredSession session) {
        cmdRunner.updateSession(clientId, session);
    }

    @Override
    public void updateSession(String clientId, StoredSession session) {
        cmdRunner.updateSession(clientId, session);
    }

    @Override
    public void removeSession(String clientId) {
        String key = String.format(RedisConstant.SESSION_KEY_FORMAT, clientId);
        cmdRunner.del(key);
    }

    @Override
    public boolean contain(String clientId) {
        String key = String.format(RedisConstant.SESSION_KEY_FORMAT, clientId);
        return cmdRunner.isExistKey(key);
    }
}
