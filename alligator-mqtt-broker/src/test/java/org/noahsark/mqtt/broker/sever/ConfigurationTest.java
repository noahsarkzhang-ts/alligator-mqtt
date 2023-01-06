package org.noahsark.mqtt.broker.sever;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * Configuration 测试类
 *
 * @author zhangxt
 * @date 2023/01/03 11:52
 **/
public class ConfigurationTest {

    private static final String CONFIG_PATH = "app.properties";

    @Test
    public void configurationTest() {

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        final URL resource = contextClassLoader.getResource(CONFIG_PATH);

        System.out.println("resource:" + resource.toString());

    }

    private Configuration getConfiguration(String path) {

        Configurations configs = new Configurations();
        Configuration config = null;
        try {
            config = configs.properties(new File(path));
        } catch (ConfigurationException cex) {
            cex.printStackTrace();
        }

        return config;
    }
}
