package org.noahsark.mqtt.broker.repository.redis;

/**
 * Redis 常量类
 *
 * @author zhangxt
 * @date 2023/01/18 14:03
 **/
public class RedisConstant {

    /**
     * Client Session 的 redis key
     * 格式：s:ol:{clientId}
     * 数据类型：String
     */
    public static final String SESSION_KEY_FORMAT = "s:ol:%s";

    /**
     * 存放发送中的 QOS1&2 PublishMessage 消息
     * 格式：s:f:p:{clientId}
     * 数据类型：Hash,{key=packetId, value=message}
     */
    public static final String SESSION_INFLIGHT_KEY_FORMAT = "s:f:p:%s";

    /**
     * 存放发送中的 QOS2 PubRel 消息
     * 格式：s:f:r:{clientId}
     * 数据类型：Set,{packetId...}
     */
    public static final String SESSION_PUBREL_KEY_FORMAT = "s:f:r:%s";

    /**
     * 存放收到的 QOS1&2 PublishMessage 消息
     * 格式：s:f:r:{clientId}
     * 数据类型：Hash,{key=packetId, value=message}
     */
    public static final String SESSION_RECEIVE_KEY_FORMAT = "s:f:ri:%s";

    /**
     * 存放 QOS1&2 级别 Topic 的 offset 位置
     * 格式：s:t:{clientId}
     * 数据类型：Hash,{key=topic, value=offset}
     */
    public static final String SESSION_TOPIC_OFFSET_FORMAT = "s:t:%s";

    /**
     * 存放客户端的订阅关系
     * 格式：s:s:{clientId}
     * 数据类型：Hash,{key=topic, value=subscription}
     */
    public static final String SESSION_SUBSCRIPTION_FORMAT = "s:s:%s";

    /**
     * 存放客户端的 Will 信息
     * 格式：s:t:{clientId}
     * 数据类型：String
     */
    public static final String SESSION_WILL_FORMAT = "s:w:%s";

    /**
     * 存放 Topic 的 Retain 信息
     * 格式：t:r:{topic}
     * 数据类型：List
     */
    public static final String TOPIC_RETAIN_FORMAT = "t:r:%s";

    /**
     * 存放 Topic 当前 offset
     * 格式：t:o:{topic}
     * 数据类型：String(int)
     */
    public static final String TOPIC_OFFSET_FORMAT = "t:o:%s";


}
