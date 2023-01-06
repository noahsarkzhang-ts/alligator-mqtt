package org.noahsark.mqtt.broker.sever;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Test;
import org.noahsark.mqtt.broker.Server;
import org.noahsark.mqtt.broker.clusters.MqttEventBusManager;
import org.noahsark.mqtt.broker.common.factory.MqttBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 集群功能测试
 *
 * @author zhangxt
 * @date 2022/12/14 10:56
 **/
public class ClusterGridTest {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterGridTest.class);

    private static final String NODE1_CONFIG = "config/alligator1.properties";

    private static final String NODE2_CONFIG = "config/alligator2.properties";

    @Test
    public void testMqttClusterGrid1() {
        MqttEventBusManager node1 = MqttBeanFactory.getInstance().mqttEventBusManager();
        Configuration configuration = getConfiguration(NODE1_CONFIG);

        node1.load(configuration);

        node1.dump();

        node1.startup();

        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testMqttClusterGrid2() {
        MqttEventBusManager node2 = MqttBeanFactory.getInstance().mqttEventBusManager();
        Configuration configuration = getConfiguration(NODE2_CONFIG);

        node2.load(configuration);

        node2.dump();

        node2.startup();

        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testNode1() {
        Server server = new Server();

        String [] args = {"-f","E:\\git-repository\\alligator-mqtt\\alligator-mqtt-broker\\src\\test\\resources\\config\\alligator1.properties"};

        server.start(args);

        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNode2() {
        Server server = new Server();

        String [] args = {"-f","E:\\git-repository\\alligator-mqtt\\alligator-mqtt-broker\\src\\test\\resources\\config\\alligator2.properties"};

        server.start(args);

        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNodeStart() {
        String [] args = {"-f","E:\\git-repository\\alligator-mqtt\\alligator-mqtt-broker\\src\\test\\resources\\config\\alligator.properties"};
        // String [] args = {};

        Server server = new Server();
        server.start(args);

        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private Configuration getConfiguration(String fileName) {

        Configurations configs = new Configurations();
        Configuration config = null;
        try {
            config = configs.properties(new File(fileName));
        } catch (ConfigurationException cex) {
            LOG.warn("Config File parse failed!", cex);
        }

        return config;
    }
}
