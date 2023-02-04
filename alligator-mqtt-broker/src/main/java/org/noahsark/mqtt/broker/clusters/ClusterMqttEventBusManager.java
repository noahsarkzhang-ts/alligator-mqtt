package org.noahsark.mqtt.broker.clusters;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.noahsark.mqtt.broker.clusters.entity.ClusterSubscriptionInfo;
import org.noahsark.mqtt.broker.clusters.entity.MqttServerInfo;
import org.noahsark.mqtt.broker.clusters.entity.ServerLoginfo;
import org.noahsark.mqtt.broker.clusters.entity.ServerSubject;
import org.noahsark.mqtt.broker.clusters.processor.ClusterClientLogoutProcessor;
import org.noahsark.mqtt.broker.clusters.processor.ClusterPublishProcessor;
import org.noahsark.mqtt.broker.clusters.processor.ClusterSubscriptionProcessor;
import org.noahsark.mqtt.broker.clusters.processor.ServerLoginProcessor;
import org.noahsark.mqtt.broker.clusters.serializer.ProtobufSerializer;
import org.noahsark.rpc.common.constant.SerializerType;
import org.noahsark.rpc.common.remote.CommandCallback;
import org.noahsark.rpc.common.remote.Request;
import org.noahsark.rpc.common.serializer.SerializerManager;
import org.noahsark.rpc.socket.event.ClientConnectionSuccessEvent;
import org.noahsark.rpc.socket.event.ClientDisconnectEvent;
import org.noahsark.rpc.socket.eventbus.ApplicationListener;
import org.noahsark.rpc.socket.eventbus.EventBus;
import org.noahsark.rpc.socket.session.Session;
import org.noahsark.rpc.socket.tcp.client.TcpClient;
import org.noahsark.rpc.socket.tcp.server.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mqtt 网络集群
 *
 * @author zhangxt
 * @date 2022/12/07 10:37
 **/
public class ClusterMqttEventBusManager implements MqttEventBusManager {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterMqttEventBusManager.class);

    // topic 在服务器上的分布关系，topic-->服务器集合
    private Map<String, Set<Integer>> topicHolders = new HashMap<>();

    // 与其它服务器的会话信息
    private Map<Integer, Session> serverSessions = new HashMap<>();

    // 集群中所有服务器信息
    private Map<Integer, MqttServerInfo> servers = new HashMap<>();

    // 当前服务器结点
    private MqttServerInfo currentServer;

    private TcpServer mqttServer;

    // 与其它服务器的客户端
    private Map<Integer, TcpClient> clients = new HashMap<>();

    private int serverSize;

    private HealthStatus health = HealthStatus.RED;

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public ClusterMqttEventBusManager() {
    }

    /**
     * 加载集群配置
     *
     * @param configuration 配置信息
     */
    @Override
    public void load(Configuration configuration) {

        int id = configuration.getInt("server.id");
        LOG.info("id:{}", id);

        Iterator<String> keys = configuration.getKeys("server");
        String key;
        int size = 0;
        while (keys.hasNext()) {
            key = keys.next();
            // transport.{1..2} = ip:port
            String num = key.split("\\.")[1];
            if (StringUtils.isNumeric(num)) {
                String value = configuration.getString(key);
                String[] parts = value.split(":");

                MqttServerInfo serverInfo = new MqttServerInfo(Integer.parseInt(num), parts[0],
                        Integer.parseInt(parts[1]));

                servers.put(Integer.parseInt(num), serverInfo);
                size++;
            }
        }

        currentServer = servers.get(id);
        serverSize = size;
    }

    @Override
    public void init() {
    }

    @Override
    public String alias() {
        return "cluster";
    }

    @Override
    public void startup() {

        // 0. 注册序列化器及处理器
        register();

        // 1. 启动当前结点
        startupServer();

        // 2.注册连接回调处理函数
        registerClientListener();

        // 2.与其它服务器建立连接
        buildConnect();

        executor.scheduleAtFixedRate(this::dump, 10, 60, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void addServerSession(Integer index, Session session) {
        serverSessions.put(index, session);
        checkHealth();
    }

    @Override
    public void subscription(ClusterSubscriptionInfo info) {
        Integer serverId = info.getServerId();

        Set<String> additions = info.getAddition();
        Set<String> removes = info.getRemove();

        additions.forEach(token -> {
            Set<Integer> serverSet = topicHolders.get(token);
            if (serverSet != null) {
                serverSet = new HashSet<>();
                topicHolders.put(token, serverSet);
            }

            serverSet.add(serverId);
        });

        removes.forEach(token -> {
            Set<Integer> serverSet = topicHolders.get(token);
            if (serverSet != null) {
                serverSet.remove(serverId);
            }
        });

    }

    @Override
    public Map<String, Set<Integer>> getTopicHolders() {
        return this.topicHolders;
    }

    @Override
    public Map<Integer, Session> traverseSessions() {
        return serverSessions;
    }

    @Override
    public MqttServerInfo getCurrentServer() {
        return this.currentServer;
    }

    @Override
    public String getClusterModel() {
        return "cluster";
    }

    private void register() {
        // 1. 注册 protobuf 序列化器
        ProtobufSerializer protobufSerializer = new ProtobufSerializer();
        SerializerManager.getInstance().register(SerializerType.PROBUFFER, protobufSerializer);

        // 2. 注册登陆处理器
        ServerLoginProcessor loginProcessor = new ServerLoginProcessor();
        loginProcessor.register();

        // 3. 注册 广播 Publish 处理器
        ClusterPublishProcessor publishProcessor = new ClusterPublishProcessor();
        publishProcessor.register();

        // 4. 注册 广播 Subscription 处理器
        ClusterSubscriptionProcessor subscriptionProcessor = new ClusterSubscriptionProcessor();
        subscriptionProcessor.register();

        // 5. 注册 Client 下线处理器
        ClusterClientLogoutProcessor logoutProcessor = new ClusterClientLogoutProcessor();
        logoutProcessor.register();
    }

    private void startupServer() {
        mqttServer = new TcpServer(currentServer.getIp(), currentServer.getPort());

        mqttServer.start();
    }

    private void buildConnect() {
        // TODO 建立与其它服务器之间的会话连接
        Set<Integer> keys = servers.keySet();

        keys.stream().filter(item -> item > currentServer.getId()).forEach(item -> {
            MqttServerInfo serverInfo = servers.get(item);

            TcpClient tcpClient = new TcpClient(serverInfo.getIp(), serverInfo.getPort());
            tcpClient.connect(item);

            clients.put(item, tcpClient);

        });
    }

    private void registerClientListener() {
        EventBus.getInstance().register(new ClientConnectionSuccessListener());
        EventBus.getInstance().register(new ClientDisconnectionSuccessListener());
    }

    private void checkHealth() {
        int unhealthNum = 0;
        int heathNum = 0;

        final Iterator<Map.Entry<Integer, Session>> iterator = serverSessions.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Integer, Session> entry = iterator.next();
            Session session = entry.getValue();

            if (Session.SessionStatus.AUTHORIZED.equals(session.getStatus())) {
                heathNum++;
            } else {
                unhealthNum++;
            }
        }

        int outsides = serverSize - 1;

        if (heathNum == outsides) {
            health = HealthStatus.GREEN;

            return;
        }

        if (unhealthNum == outsides || serverSessions.size() == 0) {
            health = HealthStatus.RED;
            return;
        }

        health = HealthStatus.YELLOW;

    }

    @Override
    public void dump() {
        dumpServer();
        dumpSession();

    }

    public void dumpServer() {
        LOG.info("************Cluster*****************");
        StringBuffer buffer = new StringBuffer();
        buffer.append("Cluster size :" + serverSize + ",current node:" + currentServer.getId()
                + ",Health: " + health.toString() + "\n");
        buffer.append("Cluster Node:");
        servers.forEach((id, serverInfo) -> buffer.append("\ntransport " + id + " : " + serverInfo.getIp() + ":" + serverInfo.getPort()));

        LOG.info(buffer.toString());

        LOG.info("************Cluster*****************");
    }

    public void dumpSession() {

        LOG.info("************Cluster Session *****************");
        StringBuffer buffer = new StringBuffer();
        buffer.append("Session size :" + serverSessions.size() + "\n");
        buffer.append("Cluster Session:");
        serverSessions.forEach((id, session) -> buffer.append("\ntransport( " + currentServer.getId() + "-->" + id + " ): " + session.getSessionId() + ":" + session.getStatus()));

        LOG.info(buffer.toString());

        LOG.info("************Cluster Session *****************");
    }


    private class ClientConnectionSuccessListener extends ApplicationListener<ClientConnectionSuccessEvent> {

        @Override
        public void onApplicationEvent(ClientConnectionSuccessEvent clientConnectionSuccessEvent) {

            Integer index = (Integer) clientConnectionSuccessEvent.getSource();
            TcpClient client = clients.get(index);
            Session session = client.getSession();
            session.setEndpointType(Session.SessionEndpointType.CLIENT);

            ServerSubject subject = new ServerSubject(index.toString());
            session.setSubject(subject);

            serverSessions.put(index, session);

            // TODO 发起登陆验证功能
            Request request = buildLoginRequest();
            // session.invoke

            session.invoke(request, new CommandCallback() {
                @Override
                public void callback(Object result, int currentFanout, int fanout) {
                    LOG.info("Node {} --> Node {} session has built!", currentServer.getId(), index);

                    session.setStatus(Session.SessionStatus.AUTHORIZED);

                    checkHealth();
                }

                @Override
                public void failure(Throwable cause, int currentFanout, int fanout) {
                    LOG.warn("Node {} --> Node {} session was failed!", currentServer.getId(), index);

                    checkHealth();
                }


            }, 300000);

        }

        private Request buildLoginRequest() {
            ServerLoginfo serverLoginfo = new ServerLoginfo(currentServer.getId(), "token");

            Request request = new Request.Builder()
                    .biz(5)
                    .cmd(100)
                    .serializer(SerializerType.PROBUFFER)
                    .payload(serverLoginfo)
                    .build();

            return request;
        }
    }

    private class ClientDisconnectionSuccessListener extends ApplicationListener<ClientDisconnectEvent> {

        @Override
        public void onApplicationEvent(ClientDisconnectEvent clientDisconnectEvent) {

            ServerSubject subject = (ServerSubject) clientDisconnectEvent.getSource();

            if (subject == null) {
                return;
            }

            Integer serverId = Integer.parseInt(subject.getId());
            serverSessions.remove(serverId);

            checkHealth();
        }
    }

    public enum HealthStatus {
        GREEN, // 所有节点在线
        YELLOW, // 至少一个节点下线
        RED // 除了本节点，其它所有结点下线
    }

}

