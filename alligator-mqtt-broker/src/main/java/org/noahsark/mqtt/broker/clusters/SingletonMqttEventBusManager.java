package org.noahsark.mqtt.broker.clusters;

import org.apache.commons.configuration2.Configuration;
import org.noahsark.mqtt.broker.clusters.entity.ClusterSubscriptionInfo;
import org.noahsark.mqtt.broker.clusters.entity.MqttServerInfo;
import org.noahsark.mqtt.broker.common.exception.OprationNotSupportedException;
import org.noahsark.rpc.socket.session.Session;

import java.util.Map;
import java.util.Set;

/**
 * 单实例事件管理器
 *
 * @author zhangxt
 * @date 2023/01/04 18:06
 **/
public class SingletonMqttEventBusManager implements MqttEventBusManager {

    @Override
    public void addServerSession(Integer index, Session session) {
        throw new OprationNotSupportedException();
    }

    @Override
    public void subscription(ClusterSubscriptionInfo info) {
        throw new OprationNotSupportedException();
    }

    @Override
    public Map<String, Set<Integer>> getTopicHolders() {
        throw new OprationNotSupportedException();
    }

    @Override
    public Map<Integer, Session> traverseSessions() {
        throw new OprationNotSupportedException();
    }

    @Override
    public MqttServerInfo getCurrentServer() {
        throw new OprationNotSupportedException();
    }

    @Override
    public void dump() {
    }

    @Override
    public void load(Configuration configuration) {

    }

    @Override
    public void startup() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void init() {

    }

    @Override
    public String alias() {
        return "singleton";
    }
}
