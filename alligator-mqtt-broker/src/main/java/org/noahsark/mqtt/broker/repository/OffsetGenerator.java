package org.noahsark.mqtt.broker.repository;

/**
 * Offset 生成器
 *
 * @author zhangxt
 * @date 2023/01/30 10:12
 **/
public interface OffsetGenerator {

    /**
     * 获取指定 topic 的下一个全局id
     *
     * @param topic 消息主题
     * @return 下一个全局id
     */
    long incrOffset(String topic);

    /**
     * 重置 topic offset
     *
     * @param topic  消息主题
     * @param offset 重置的 offset
     * @return 重置后的offset
     */
    void resetOffset(String topic, long offset);
}
