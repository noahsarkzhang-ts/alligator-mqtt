package org.noahsark.mqtt.broker.sever;

import org.apache.commons.configuration2.Configuration;
import org.junit.Test;
import org.noahsark.mqtt.broker.Server;

/**
 * Server Test
 *
 * @author zhangxt
 * @date 2022/11/08 17:58
 **/
public class ServerTest {

    @Test
    public void configurationTest() {
        Server server = new Server();

        String[] args = {"-f", "E:\\git-repository\\alligator-mqtt\\alligator-mqtt-broker\\src\\main\\resources\\config\\alligator2.properties"};

        System.out.println("Server Started......");

        server.start(args);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        /*Configuration configuration = server.getConfiguration(null);

        System.out.println(configuration.getString("host"));*/
    }
}
