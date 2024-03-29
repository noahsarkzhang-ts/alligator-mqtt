package org.noahsark.mqtt.broker.common.redis.executor;

import com.google.common.base.Preconditions;

import org.noahsark.mqtt.broker.clusters.serializer.ProtobufSerializer;
import org.noahsark.mqtt.broker.clusters.serializer.ProtostuffUtils;
import org.noahsark.mqtt.broker.common.redis.cmd.CmdEnum;
import org.noahsark.mqtt.broker.common.redis.cmd.OperationEnum;
import org.noahsark.mqtt.broker.common.redis.cmd.RedisCmd;
import org.noahsark.mqtt.broker.common.redis.cmd.Triple;
import org.noahsark.mqtt.broker.protocol.entity.PublishInnerMessage;
import org.noahsark.mqtt.broker.protocol.entity.RetainedMessage;
import org.noahsark.mqtt.broker.protocol.entity.Will;
import org.noahsark.mqtt.broker.repository.entity.StoredSession;
import org.noahsark.mqtt.broker.repository.entity.StoredSubscription;
import org.noahsark.mqtt.broker.repository.redis.RedisConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.params.SetParams;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 命令执行类
 *
 * @author zhangxt
 * @date 2020/3/8
 */
public class RedisCmdRunner {

    private static Logger logger = LoggerFactory.getLogger(RedisCmdRunner.class);

    public static final String SET_SUCCESS = "OK";

    private JedisPoolAbstract jedisPool;

    public RedisCmdRunner(JedisPoolAbstract jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 批量执行管道命令
     *
     * @param cmds 命令集合
     */
    public void exePipeline(List<RedisCmd> cmds) {
        Preconditions.checkNotNull(cmds);

        logger.debug("exe cmds: {}", cmds);

        try (Jedis jedis = jedisPool.getResource()) {

            // 1、生成pipeline对象
            Pipeline pipeline = jedis.pipelined();

            // 2、解析cmd命令
            cmds.forEach(cmd -> cmd.parseCmd(pipeline));

            // 3、执行命令
            pipeline.sync();

        } catch (Exception ex) {
            logger.error("catch an exception when executing pipiline!", ex);

            throw new IllegalStateException(ex);
        }

    }

    /**
     * 执行单条命令
     *
     * @param cmd 命令
     */
    public void exeCmd(RedisCmd cmd) {

        List<RedisCmd> cmds = new ArrayList<>();
        cmds.add(cmd);

        exeCmds(cmds);
    }

    /**
     * 批量执行命令
     *
     * @param cmds 命令列表
     */
    public void exeCmds(List<RedisCmd> cmds) {
        Preconditions.checkNotNull(cmds);

        logger.debug("exe cmds: {}", cmds);

        try (Jedis jedis = jedisPool.getResource()) {

            // 1、生成pipeline对象
            Pipeline pipeline = jedis.pipelined();

            int count = 0;
            List<Triple> elements;
            for (RedisCmd cmd : cmds) {

                if (CmdEnum.STRING.equals(cmd.getType())) {
                    cmd.parseString(pipeline);
                    count++;
                } else {
                    if (cmd.parseDel(pipeline) == 1) {
                        count++;
                        continue;
                    }

                    elements = cmd.getElements();
                    if (elements == null || elements.size() == 0) {
                        continue;
                    }

                    for (Triple element : elements) {
                        if (count > 100) {
                            // 每一次批量执行100条命令
                            pipeline.sync();
                            count = 0;

                            pipeline = jedis.pipelined();
                        }

                        cmd.parseCmdElement(pipeline, element);
                        count++;
                    }

                    cmd.parseExprie(pipeline);
                    count++;
                }

                if (count > 100) {
                    // 每一次批量执行100条命令
                    pipeline.sync();
                    count = 0;

                    pipeline = jedis.pipelined();
                }
            }

            // 3、执行最后一次删除
            if (count > 0) {
                pipeline.sync();
            }

        } catch (Exception ex) {
            logger.error("catch an exception when executing pipiline!", ex);
        }


    }

    /**
     * 获取hash key元素个数
     *
     * @param key
     * @return
     */
    public Long getHlen(String key) {
        Preconditions.checkNotNull(key);

        try (Jedis jedis = jedisPool.getResource()) {

            return jedis.hlen(key);

        } catch (Exception ex) {
            logger.error("catch an exception when adding token!", ex);
        }

        return 0L;
    }

    /**
     * 判断set中是否包含元素
     *
     * @param key     key
     * @param element 元素
     * @return 结果
     */
    public boolean isExistInSet(String key, String element) {

        boolean result = false;

        try (Jedis jedis = jedisPool.getResource()) {
            result = jedis.sismember(key, element);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }

        return result;
    }

    public boolean isExistInHash(String key, String field) {

        boolean result = false;

        try (Jedis jedis = jedisPool.getResource()) {
            result = jedis.hexists(key, field);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }

        return result;
    }

    /**
     * 判断元素是否在zset中
     *
     * @param key     key
     * @param element 元素
     * @return 结果
     */
    public boolean isExistInZSet(String key, String element) {

        boolean result = false;

        try (Jedis jedis = jedisPool.getResource()) {
            Double score = jedis.zscore(key, element);

            if (score != null) {
                result = true;
            }
        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }

        return result;
    }

    /**
     * 是否存在一个 key
     *
     * @param key key
     * @return boolean
     * @author zhangxt
     * @date 2021/11/06 20:24
     */
    public boolean isExistKey(String key) {
        boolean result = false;

        try (Jedis jedis = jedisPool.getResource()) {
            result = jedis.exists(key);
        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }

        return result;
    }

    public void del(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }

    public void hdel(String key, String field) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(key, field);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }

    public void delElementFromSet(String key, String element) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.srem(key, element);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }

    public void addElementFromSet(String key, String element) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sadd(key, element);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }

    public Long incr(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr(key);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }

        return 0L;
    }

    /**
     * 返回zset集合下的指定分数的信息
     *
     * @param key   key
     * @param start start
     * @param end   end
     * @return list
     */
    public List<String> getElementsFromZset(String key, double start, double end) {

        long startTime = System.currentTimeMillis();

        List<String> list = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {

            Long length = jedis.zcount(key, start, end);
            if (length == null || length == 0) {
                return list;
            }

            int offset;
            int count = 100;

            // 分页获取，一页100个元素
            int page = length.intValue() / count;
            if (length.intValue() % count != 0) {
                page++;
            }

            Set<String> frgment;
            for (int i = 1; i <= page; i++) {
                offset = (i - 1) * count;
                frgment = jedis.zrangeByScore(key, start, end, offset, count);

                list.addAll(frgment);
            }

            logger.info("query zset data, length : {}, time : {}", length, (System.currentTimeMillis() - startTime));

        } catch (Exception ex) {
            logger.error("catch an exception when adding token!", ex);
        }

        return list;

    }

    /**
     * 返回set集合下的所有元素
     *
     * @param key key
     * @return list
     */
    public List<String> getElementsFromSet(String key) {

        long startTime = System.currentTimeMillis();

        List<String> list = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {

            // 游标初始值为0
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams scanParams = new ScanParams();
            scanParams.count(100);
            ScanResult<String> sscanResult;
            List<String> scanResult;
            do {

                //使用sscan命令获取100条数据，使用cursor游标记录位置，下次循环使用
                sscanResult = jedis.sscan(key, cursor, scanParams);

                // 返回0,说明遍历完成
                cursor = sscanResult.getCursor();

                scanResult = sscanResult.getResult();
                list.addAll(scanResult);

            } while (!"0".equals(cursor));

            logger.info("query set data, length : {}, time : {}", list.size(), (System.currentTimeMillis() - startTime));

        } catch (Exception ex) {
            logger.error("catch an exception when adding token!", ex);
        }

        return list;

    }

    public void batchDelElementFromSet(boolean isSet, String key, List<String> elements) {
        try (Jedis jedis = jedisPool.getResource()) {

            // 1、生成pipeline对象
            Pipeline pipeline = jedis.pipelined();

            // 2、解析cmd命令
            elements.forEach(element -> {
                if (isSet) {
                    pipeline.srem(key, element);
                } else {
                    pipeline.zrem(key, element);
                }
            });

            // 3、执行命令
            pipeline.sync();

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }

    public void batchAddElementFromSet(String key, List<String> elements) {
        try (Jedis jedis = jedisPool.getResource()) {

            // 1、生成pipeline对象
            Pipeline pipeline = jedis.pipelined();

            // 2、解析cmd命令
            elements.forEach(element -> pipeline.sadd(key, element));

            // 3、执行命令
            pipeline.sync();

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }


    public BigDecimal getNumValue(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            Long val = jedis.scard(key);
            if (val == null) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(val);
        }
    }

    public BigDecimal getHashNumValue(String key, String filed) {
        try (Jedis jedis = jedisPool.getResource()) {
            String val = jedis.hget(key, filed);
            if (val == null) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(val);
        }
    }

    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);

        } catch (Exception ex) {
            logger.error("catch an exception when set element!", ex);
        }
    }

    public void set(String key, byte[] value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key.getBytes(), value);

        } catch (Exception ex) {
            logger.error("catch an exception when set element!", ex);
        }
    }

    public byte[] get(String key) {

        byte[] value = null;

        try (Jedis jedis = jedisPool.getResource()) {
            value = jedis.get(key.getBytes());

        } catch (Exception ex) {
            logger.error("catch an exception when set element!", ex);
        }

        return value;
    }

    public boolean set(String key, String value, SetParams params) {
        try (Jedis jedis = jedisPool.getResource()) {
            String res = jedis.set(key, value, params);
            return SET_SUCCESS.equals(res);
        } catch (Exception ex) {
            logger.error("catch an exception when set element!", ex);
            return false;
        }
    }

    public Long scard(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.scard(key);
        } catch (Exception ex) {
            logger.error("catch an exception when set element!", ex);
        }

        return 0L;
    }

    public List<String> moveSet(String source, String target) {

        List<String> list = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {

            // 每一次移动100条
            ScanParams scanParams = new ScanParams();
            scanParams.count(100);

            // 游标初始值为0
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanResult<String> sscanResult;
            List<String> scanResult;
            String[] part;

            long start = 0L;
            long end = 0L;

            logger.info("move tokens, source: {}, targer: {}", source, target);

            do {

                start = System.currentTimeMillis();
                //使用sscan命令获取100条数据，使用cursor游标记录位置，下次循环使用
                sscanResult = jedis.sscan(source, cursor, scanParams);

                cursor = sscanResult.getCursor();

                scanResult = sscanResult.getResult();
                if (scanResult != null && scanResult.size() > 0) {

                    part = scanResult.toArray(new String[0]);
                    jedis.srem(source, part);
                    jedis.sadd(target, part);

                    list.addAll(scanResult);
                }
                end = System.currentTimeMillis();

                logger.info("move of element in set : {}, time(ms): {}", scanResult.size(), (end - start));

                // 返回0,说明遍历完成ss
            } while (!"0".equals(cursor));

        } catch (Exception ex) {
            logger.error("catch an exception when adding token!", ex);
        }

        return list;

    }

    public StoredSession getSession(String clientId) {
        Preconditions.checkNotNull(clientId);

        StoredSession session = null;
        try (Jedis jedis = jedisPool.getResource()) {
            String key = String.format(RedisConstant.SESSION_KEY_FORMAT, clientId);
            byte[] value = jedis.get(key.getBytes());

            if (value != null && value.length > 0) {
                session = ProtostuffUtils.deserialize(value, StoredSession.class);
            }

        } catch (Exception ex) {
            logger.error("catch an exception when adding session!", ex);
        }

        return session;
    }

    public <T> T get(String key, Class<T> classz) {
        Preconditions.checkNotNull(key);

        T result = null;
        try (Jedis jedis = jedisPool.getResource()) {

            byte[] value = jedis.get(key.getBytes());

            if (value != null && value.length > 0) {
                result = ProtostuffUtils.deserialize(value, classz);
            }

        } catch (Exception ex) {
            logger.error("catch an exception when adding session!", ex);
        }

        return result;
    }

    public void updateSession(String clientId, StoredSession session) {
        Preconditions.checkNotNull(clientId);
        Preconditions.checkNotNull(session);

        RedisCmd sessionCmd = new RedisCmd();
        sessionCmd.setType(CmdEnum.STRING);
        sessionCmd.setOperationType(OperationEnum.UPDATE);

        Triple key = new Triple();
        key.setKey(String.format(RedisConstant.SESSION_KEY_FORMAT, clientId));
        key.setValue(ProtostuffUtils.serialize(session));

        sessionCmd.setKey(key);

        exeCmd(sessionCmd);

    }

    public void updateMessage(String clientId, PublishInnerMessage message, boolean isSending) {

        Preconditions.checkNotNull(clientId);
        Preconditions.checkNotNull(message);

        RedisCmd messageCmd = new RedisCmd();
        messageCmd.setType(CmdEnum.HASH);
        messageCmd.setOperationType(OperationEnum.UPDATE);

        Triple key = new Triple();
        if (isSending) {
            key.setKey(String.format(RedisConstant.SESSION_INFLIGHT_KEY_FORMAT, clientId));
        } else {
            key.setKey(String.format(RedisConstant.SESSION_RECEIVE_KEY_FORMAT, clientId));
        }
        messageCmd.setKey(key);

        Triple element = new Triple();
        element.setKey(String.valueOf(message.getMessageId()));
        element.setValue(ProtostuffUtils.serialize(message));
        messageCmd.addElement(element);

        exeCmd(messageCmd);
    }

    public PublishInnerMessage getMessage(String clientId, int packetId, boolean isSending) {
        Preconditions.checkNotNull(clientId);

        PublishInnerMessage message = null;
        try (Jedis jedis = jedisPool.getResource()) {

            String key;
            if (isSending) {
                key = String.format(RedisConstant.SESSION_INFLIGHT_KEY_FORMAT, clientId);
            } else {
                key = String.format(RedisConstant.SESSION_RECEIVE_KEY_FORMAT, clientId);
            }

            byte[] value = jedis.hget(key.getBytes(), Integer.toString(packetId).getBytes());

            if (value != null && value.length > 0) {
                message = ProtostuffUtils.deserialize(value, PublishInnerMessage.class);
            }

        } catch (Exception ex) {
            logger.error("catch an exception when get message!", ex);
        }

        return message;
    }

    public List<PublishInnerMessage> getAllMessage(String clientId, boolean isSending) {
        Preconditions.checkNotNull(clientId);

        List<PublishInnerMessage> list = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {

            // 游标初始值为0
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams scanParams = new ScanParams();
            scanParams.count(20);
            String key;
            if (isSending) {
                key = String.format(RedisConstant.SESSION_INFLIGHT_KEY_FORMAT, clientId);
            } else {
                key = String.format(RedisConstant.SESSION_RECEIVE_KEY_FORMAT, clientId);
            }

            ScanResult<Map.Entry<byte[], byte[]>> hscanResult;
            List<Map.Entry<byte[], byte[]>> scanResult;

            long start = 0L;
            long end = 0L;

            do {

                start = System.currentTimeMillis();
                //使用hscan命令获取20条数据，使用cursor游标记录位置，下次循环使用
                hscanResult = jedis.hscan(key.getBytes(), cursor.getBytes(), scanParams);
                // 返回0,说明遍历完成
                cursor = hscanResult.getCursor();
                scanResult = hscanResult.getResult();

                for (Map.Entry<byte[], byte[]> entry : scanResult) {
                    list.add(ProtostuffUtils.deserialize(entry.getValue(), PublishInnerMessage.class));
                }
                end = System.currentTimeMillis();

                logger.info("size of messages: {}, time(ms): {}", scanResult.size(), (end - start));

            } while (!"0".equals(cursor));

        } catch (Exception ex) {
            logger.error("catch an exception when getting messages!", ex);
        }

        return list;
    }

    public void hset(String key, String field, byte[] value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(key.getBytes(), field.getBytes(), value);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }

    public Map<String, Long> getAllTopicOffsets(String clientId) {
        Preconditions.checkNotNull(clientId);

        Map<String, Long> entries = new HashMap<>();

        try (Jedis jedis = jedisPool.getResource()) {

            // 游标初始值为0
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams scanParams = new ScanParams();
            scanParams.count(20);
            String key = String.format(RedisConstant.SESSION_TOPIC_OFFSET_FORMAT, clientId);

            ScanResult<Map.Entry<String, String>> hscanResult;
            List<Map.Entry<String, String>> scanResult;

            long start = 0L;
            long end = 0L;

            do {

                start = System.currentTimeMillis();
                //使用hscan命令获取20条数据，使用cursor游标记录位置，下次循环使用
                hscanResult = jedis.hscan(key, cursor, scanParams);
                // 返回0,说明遍历完成
                cursor = hscanResult.getCursor();
                scanResult = hscanResult.getResult();

                for (Map.Entry<String, String> entry : scanResult) {
                    entries.put(entry.getKey(), Long.parseLong(entry.getValue()));
                }
                end = System.currentTimeMillis();

                logger.info("size of messages: {}, time(ms): {}", scanResult.size(), (end - start));

            } while (!"0".equals(cursor));

        } catch (Exception ex) {
            logger.error("catch an exception when getting topic offset!", ex);
        }

        return entries;
    }

    public void sadd(String key, byte[] value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sadd(key.getBytes(), value);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }

    public void rpush(String key, byte[] value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.rpush(key.getBytes(), value);

        } catch (Exception ex) {
            logger.error("catch an exception when existing element!", ex);
        }
    }

    public List<RetainedMessage> getAllRetainMessage(String topic) {
        Preconditions.checkNotNull(topic);

        List<RetainedMessage> list = new ArrayList<>();
        String key = String.format(RedisConstant.TOPIC_RETAIN_FORMAT, topic);

        try (Jedis jedis = jedisPool.getResource()) {
            // TODO
            Long len = jedis.llen(key);

            List<byte[]> results = jedis.lrange(key.getBytes(), 0, len - 1);
            results.forEach(element -> {
                list.add(ProtostuffUtils.deserialize(element, RetainedMessage.class));
            });

        } catch (Exception ex) {
            logger.error("catch an exception when getting messages!", ex);
        }

        return list;
    }

    public List<StoredSubscription> getAllSubscriptions(String clientId) {
        Preconditions.checkNotNull(clientId);

        List<StoredSubscription> list = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {

            // 游标初始值为0
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams scanParams = new ScanParams();
            scanParams.count(20);
            String key = String.format(RedisConstant.SESSION_SUBSCRIPTION_FORMAT, clientId);

            ScanResult<Map.Entry<byte[], byte[]>> hscanResult;
            List<Map.Entry<byte[], byte[]>> scanResult;

            long start = 0L;
            long end = 0L;

            do {

                start = System.currentTimeMillis();
                //使用hscan命令获取20条数据，使用cursor游标记录位置，下次循环使用
                hscanResult = jedis.hscan(key.getBytes(), cursor.getBytes(), scanParams);
                // 返回0,说明遍历完成
                cursor = hscanResult.getCursor();
                scanResult = hscanResult.getResult();

                for (Map.Entry<byte[], byte[]> entry : scanResult) {
                    list.add(ProtostuffUtils.deserialize(entry.getValue(), StoredSubscription.class));
                }
                end = System.currentTimeMillis();

                logger.info("size of messages: {}, time(ms): {}", scanResult.size(), (end - start));

            } while (!"0".equals(cursor));

        } catch (Exception ex) {
            logger.error("catch an exception when getting subscription!", ex);
        }

        return list;
    }

}
