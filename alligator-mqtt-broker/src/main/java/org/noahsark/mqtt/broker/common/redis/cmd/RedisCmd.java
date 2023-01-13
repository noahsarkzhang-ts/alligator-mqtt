package org.noahsark.mqtt.broker.common.redis.cmd;

import com.google.common.base.Preconditions;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis 命令
 *
 * @author zhangxt
 * @date 2020/3/9
 */
public class RedisCmd {

    /**
     * 不启用过期时间
     */
    public static final int KEY_NO_EXPIRE_SECONDS = -1;

    private Triple key;

    private List<Triple> elements;

    private CmdEnum type;

    private OperationEnum operationType;

    public RedisCmd() {
        elements = new ArrayList<>();
    }

    public void parseCmd(Pipeline pipeline) {

        if (OperationEnum.DEL.equals(operationType)) {
            pipeline.del(key.getKey());

            return;
        }

        switch (type) {
            case STRING:
                parseString(pipeline);
                break;
            case SET:
                parseSet(pipeline);
                break;
            case HASH:
                parseHash(pipeline);
                break;
            case ZSET:
                parseZset(pipeline);
                break;
            case LIST:
                parseList(pipeline);
                break;
            default:
                break;
        }
    }

    public void parseCmdElement(Pipeline pipeline, Triple element) {

        switch (type) {
            case SET:
                parseSetElement(pipeline, element);
                break;
            case HASH:
                parseHashElement(pipeline, element);
                break;
            case ZSET:
                parseZsetElement(pipeline, element);
                break;
            case LIST:
                parseListElement(pipeline, element);
                break;
            default:
                break;
        }
    }

    public void parseString(Pipeline pipeline) {
        switch (operationType) {
            case ADD:
            case UPDATE:
                pipeline.set(key.getKey().getBytes(), key.getValue());
                break;
            case COUNT_ADDITION:
                pipeline.incrBy(key.getKey(), key.getCount());
                break;
            case COUNT_MINUS:
                pipeline.decrBy(key.getKey(), key.getCount());
                break;
            case DEL:
                pipeline.del(key.getKey());
                break;
            default:
                break;
        }

        // 超时处理
        parseExprie(pipeline);
    }

    public void parseSet(Pipeline pipeline) {
        Preconditions.checkNotNull(this.elements);

        if (parseDel(pipeline) == 1) {
            return;
        }
        this.elements.forEach(element -> parseSetElement(pipeline, element));
        parseExprie(pipeline);
    }

    public void parseSetElement(Pipeline pipeline, Triple element) {

        switch (operationType) {
            case ADD:
            case COUNT_ADDITION:
                pipeline.sadd(key.getKey().getBytes(), element.getKey().getBytes());
                break;
            case REMOVE:
            case COUNT_MINUS:
                pipeline.srem(key.getKey().getBytes(), element.getKey().getBytes());
                break;
            default:
                break;
        }

    }

    public void parseZset(Pipeline pipeline) {
        Preconditions.checkNotNull(this.elements);

        if (parseDel(pipeline) == 1) {
            return;
        }
        this.elements.forEach(element -> parseZsetElement(pipeline, element));
        parseExprie(pipeline);
    }

    public void parseZsetElement(Pipeline pipeline, Triple element) {

        switch (operationType) {
            case ADD:
                pipeline.zadd(key.getKey().getBytes(), element.getScore(), element.getKey().getBytes());
                break;
            case REMOVE:
                pipeline.zrem(key.getKey().getBytes(), element.getKey().getBytes());
                break;
            default:
                break;

        }
    }

    public void parseList(Pipeline pipeline) {

    }

    public void parseListElement(Pipeline pipeline, Triple triple) {

    }

    public void parseHash(Pipeline pipeline) {
        Preconditions.checkNotNull(this.elements);

        if (parseDel(pipeline) == 1) {
            return;
        }
        this.elements.forEach(field -> parseHashElement(pipeline, field));
        parseExprie(pipeline);
    }

    public void parseHashElement(Pipeline pipeline, Triple element) {

        switch (operationType) {
            case ADD:
            case UPDATE:
                pipeline.hset(key.getKey().getBytes(), element.getKey().getBytes(), element.getValue());
                break;
            case COUNT_ADDITION:
                pipeline.hincrBy(key.getKey().getBytes(), element.getKey().getBytes(), element.getCount());
                break;
            case COUNT_MINUS:
                pipeline.hincrBy(key.getKey().getBytes(), element.getKey().getBytes(), -element.getCount());
                break;
            case REMOVE:
                pipeline.hdel(key.getKey().getBytes(), element.getKey().getBytes());
                break;
            default:
                break;
        }

    }

    public void parseCommon(Pipeline pipeline) {
        if (parseDel(pipeline) != 1) {
            parseExprie(pipeline);
        }

    }

    public int parseDel(Pipeline pipeline) {
        if (operationType == OperationEnum.DEL) {
            pipeline.del(key.getKey());
            return 1;
        }

        return 0;
    }

    public int parseExprie(Pipeline pipeline) {
        int expire = key.getExpire();
        if (expire > 0) {
            pipeline.expire(key.getKey(), expire);
            return 1;
        }

        return 0;
    }

    public static RedisCmd buildDelCmd(CmdEnum cmdEnum, String key) {
        RedisCmd cmd = new RedisCmd();
        cmd.setType(cmdEnum);
        cmd.setOperationType(OperationEnum.DEL);

        Triple pt = new Triple();
        pt.setKey(key);
        cmd.setKey(pt);

        return cmd;
    }

    public static RedisCmd buildElementCmdWithoutValue(CmdEnum cmdEnum, OperationEnum operationEnum, String key, List<String> elements) {
        RedisCmd cmd = new RedisCmd();
        cmd.setType(cmdEnum);
        cmd.setOperationType(operationEnum);

        Triple pt = new Triple();
        pt.setKey(key);
        cmd.setKey(pt);

        if (elements == null || elements.size() == 0) {
            return cmd;
        }

        Triple ele;
        for (String element : elements) {
            ele = new Triple();
            ele.setKey(element);

            cmd.addElement(ele);
        }

        return cmd;
    }

    public static RedisCmd buildSingleElementCmd(CmdEnum cmdEnum, OperationEnum operationEnum, String key, String element) {
        RedisCmd cmd = new RedisCmd();

        Triple pt = new Triple();

        cmd.setType(cmdEnum);
        cmd.setOperationType(operationEnum);

        pt.setKey(key);
        cmd.setKey(pt);

        Triple ele = new Triple();
        ele.setKey(element);
        cmd.addElement(ele);

        return cmd;
    }

    public Triple getKey() {
        return key;
    }

    public void setKey(Triple key) {
        this.key = key;
    }

    public List<Triple> getElements() {
        return elements;
    }

    public void setElements(List<Triple> elements) {
        this.elements = elements;
    }

    public CmdEnum getType() {
        return type;
    }

    public void setType(CmdEnum type) {
        this.type = type;
    }

    public OperationEnum getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationEnum operationType) {
        this.operationType = operationType;
    }

    public void addElement(Triple element) {
        this.elements.add(element);
    }

    public void addAllElement(List<Triple> elements) {
        this.elements.addAll(elements);
    }

    @Override
    public String toString() {
        return "RedisCmd{" +
                "key=" + key +
                ", elements=" + elements +
                ", type=" + type +
                ", operationType=" + operationType +
                '}';
    }

}
