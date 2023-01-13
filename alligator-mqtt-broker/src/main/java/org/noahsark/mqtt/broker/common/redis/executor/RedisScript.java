package org.noahsark.mqtt.broker.common.redis.executor;

/**
 * 脚本命令
 * @author hunter
 * @date 2021/11/25 14:29
 */
public enum RedisScript {
    /**
     * 申请锁脚本
     */
    TRY_LOCK(
            "return redis.call('SET', KEYS[1], ARGV[1], 'nx','ex', ARGV[2])"),
    /**
     * 释放锁脚本
     */
    RELEASE_LOCK(
            "if redis.call('GET',KEYS[1]) == ARGV[1] then "
                    + "  return redis.call('DEL',KEYS[1]) "
                    + "else "
                    + "  return 0 "
                    + "end");
    /**
     * 脚本内容
     */
    private final String content;

    RedisScript(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

}
