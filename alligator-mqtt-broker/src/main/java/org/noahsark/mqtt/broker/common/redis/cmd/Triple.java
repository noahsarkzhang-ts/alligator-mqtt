package org.noahsark.mqtt.broker.common.redis.cmd;

import java.util.Objects;

/**
 * Redis 元素
 *
 * @author zhangxt
 * @date 2020/3/9
 */
public class Triple {

    /**
     * 键或字段
     */
    private String key;

    /**
     * 值
     */
    private byte[] value;

    /**
     * 计数
     */
    private long count;

    /**
     * 分数，用于zset
     */
    private double score;

    /**
     * 超时时间，单位为S
     */
    private int expire;

    public Triple() {
        count = 1;
        expire = RedisCmd.KEY_NO_EXPIRE_SECONDS;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public long getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void incr() {
        this.count++;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Triple)) return false;
        Triple triple = (Triple) o;
        return getKey().equals(triple.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }

    @Override
    public String toString() {
        return "Triple{" +
                "key='" + key + '\'' +
                ", count=" + count +
                ", score=" + score +
                ", expire=" + expire +
                '}';
    }
}
