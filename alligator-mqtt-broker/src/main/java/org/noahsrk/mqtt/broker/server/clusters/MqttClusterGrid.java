package org.noahsrk.mqtt.broker.server.clusters;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.noahsark.rpc.socket.session.Session;
import org.noahsark.rpc.socket.tcp.client.TcpClient;
import org.noahsark.rpc.socket.tcp.server.TcpServer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Mqtt 网络集群
 *
 * @author zhangxt
 * @date 2022/12/07 10:37
 **/
public class MqttClusterGrid {

    // topic 在服务器上的分布关系，topic-->服务器集合
    private Map<String, Set<Integer>> topicHolders = new HashMap<>();

    // 与其它服务器的连接
    private Map<Integer, Session> serverSessions = new HashMap<>();

    // 集群中所有服务器信息
    private Map<Integer, MqttServerInfo> servers = new HashMap<>();

    // 当前服务器结点
    private MqttServerInfo currentServer;

    private TcpServer mqttServer;

    /**
     * 加载集群配置
     *
     * @param configuration 配置信息
     */
    public void load(Configuration configuration) {

        int id = configuration.getInt("server.id");

        Iterator<String> keys = configuration.getKeys("server");
        String key;
        while (keys.hasNext()) {
            key = keys.next();

            // server.{1..2} = ip:port
            String num = key.split(".")[1];
            if (StringUtils.isNumeric(num)) {
                String value = configuration.getString(key);
                String[] parts = value.split(":");

                MqttServerInfo serverInfo = new MqttServerInfo(Integer.parseInt(num), parts[0],
                        Integer.parseInt(parts[1]));

                servers.put(Integer.parseInt(num), serverInfo);
            }
        }

        currentServer = servers.get(id);
    }

    public void startup() {
        // 1. 启动当前结点
        startupServer();

        // 2. 连接其它结点
        buildNetGrid();
    }

    private void startupServer() {
        mqttServer = new TcpServer(currentServer.getIp(),currentServer.getPort());

        mqttServer.start();
    }

    private void buildNetGrid() {
        // TODO 建立与其它服务器之间的会话连接
        // 构建 serverSessions 对象；
        Set<Integer> keys = servers.keySet();

        keys.stream().filter(item -> item > currentServer.getId()).forEach(item -> {
            MqttServerInfo serverInfo = servers.get(item);

            TcpClient tcpClient = new TcpClient(serverInfo.getIp(), serverInfo.getPort());
            Session session = tcpClient.connectAndSession();

            serverSessions.put(item,session);

        });

    }

}
