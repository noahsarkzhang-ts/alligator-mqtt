package org.noahsark.mqtt.broker.common.redis.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * jedis 操作类
 *
 * @author hunter
 * @date 2021/11/25 14:29
 */
public class JedisOperate {

    private static Logger logger = LoggerFactory.getLogger(JedisOperate.class);

    protected final JedisPoolAbstract jedisPool;

    public JedisOperate(JedisPoolAbstract jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 仅执行命令,无返回值
     *
     * @param consumer
     */
    public void run(Consumer<Jedis> consumer) {
        try (Jedis jedis = jedisPool.getResource()) {
            consumer.accept(jedis);
        } catch (Exception ex) {
            logger.error("catch an exception", ex);
            throw ex;
        }
    }

    /**
     * 执行带返回值的操作
     *
     * @param function
     * @param <T>
     * @return
     */
    public <T> T run(Function<Jedis, T> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            return function.apply(jedis);
        } catch (Exception ex) {
            logger.error("catch an exception", ex);
            throw ex;
        }
    }
}
