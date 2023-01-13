package org.noahsark.mqtt.broker.common.redis.executor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import java.util.Collections;
import java.util.List;

/**
 * 脚本运行器
 * @author hunter
 * @date 2021/11/25 14:29
 */
public class RedisScriptRunner {
    private static Logger logger = LoggerFactory.getLogger(RedisScriptRunner.class);

    private LoadingCache<RedisScript, String> scriptCache;
    private JedisPoolAbstract jedisPool;
    private final JedisOperate operate;

    public RedisScriptRunner(JedisPoolAbstract jedisPool) {
        this.jedisPool = jedisPool;
        this.operate = new JedisOperate(this.jedisPool);

        loadScript();
    }

    protected void loadScript() {
        this.scriptCache = CacheBuilder.newBuilder().build(new CacheLoader<RedisScript, String>() {
            @Override
            public String load(RedisScript script) {
                return operate.run(jedis -> {
                    return jedis.scriptLoad(script.getContent());
                });
            }
        });
        operate.run(jedis -> {
            jedis.scriptFlush();
            for (RedisScript script : RedisScript.values()) {
                scriptCache.put(script, jedis.scriptLoad(script.getContent()));
            }
        });
    }

    /**
     * 申请锁
     *
     * @param lockKey 锁key
     * @param uuid    uuid
     * @param expires 过期时间,单位S
     * @return true 表示释放成功
     */
    public boolean tryLock(String lockKey, String uuid, int expires) {
        if (expires == 0) {
            expires = 10;
        }

        List<String> keys = Collections.singletonList(lockKey);
        List<String> args = Lists.newArrayList(uuid, String.valueOf(expires));
        Object result = run(RedisScript.TRY_LOCK, keys, args);

        if (result != null && "OK".equals(result)) {
            return true;
        }

        return false;
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁key
     * @param uuid    锁值
     */
    public void releaseLock(String lockKey, String uuid) {
        List<String> keys = Collections.singletonList(lockKey);
        List<String> args = Collections.singletonList(uuid);
        run(RedisScript.RELEASE_LOCK, keys, args);
    }

    /**
     * 执行脚本
     */
    public Object run(RedisScript script, List<String> keys, List<String> args) {
        try {
            final String sha1 = scriptCache.get(script);

            return operate.run(jedis -> {
                Object val;
                try {
                    val = jedis.evalsha(sha1, keys, args);
                } catch (JedisNoScriptException ex) {
                    val = jedis.eval(script.getContent(), keys, args);
                }
                return val;
            });

        } catch (Exception ex) {
            logger.warn("Catch an exception when running redis script.", ex);
        }

        return null;

    }

}
