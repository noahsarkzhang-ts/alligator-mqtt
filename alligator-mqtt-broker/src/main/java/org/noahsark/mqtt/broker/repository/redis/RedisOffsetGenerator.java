package org.noahsark.mqtt.broker.repository.redis;

import org.noahsark.mqtt.broker.common.redis.executor.RedisCmdRunner;
import org.noahsark.mqtt.broker.common.redis.executor.RedisScriptRunner;
import org.noahsark.mqtt.broker.repository.OffsetGenerator;

/**
 * Redis 版本的 Offset 生成器
 *
 * @author zhangxt
 * @date 2023/01/30 16:44
 **/
public class RedisOffsetGenerator implements OffsetGenerator {

    private RedisCmdRunner cmdRunner;

    private RedisScriptRunner scriptRunner;

    public RedisOffsetGenerator(RedisCmdRunner cmdRunner, RedisScriptRunner scriptRunner) {
        this.cmdRunner = cmdRunner;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public long incrOffset(String topic) {
        return 0;
    }

    @Override
    public void resetOffset(String topic, long offset) {

    }
}
