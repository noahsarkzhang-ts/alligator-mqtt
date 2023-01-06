package org.noahsark.mqtt.broker.clusters;

import org.noahsark.mqtt.broker.clusters.entity.ClusterSubscriptionInfo;
import org.noahsark.mqtt.broker.clusters.entity.MqttServerInfo;
import org.noahsark.mqtt.broker.common.factory.Lifecycle;
import org.noahsark.rpc.socket.session.Session;

import java.util.Map;
import java.util.Set;

/**
 * 事件总线管理器
 *
 * @author zhangxt
 * @date 2023/01/04 17:53
 **/
public interface MqttEventBusManager extends Lifecycle {

    void addServerSession(Integer index, Session session);

    void subscription(ClusterSubscriptionInfo info);

    Map<String, Set<Integer>> getTopicHolders();

    Map<Integer, Session> traverseSessions();

    MqttServerInfo getCurrentServer();

    void dump();
}
