package org.noahsark.mqtt.broker.common;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.noahsark.mqtt.broker.repository.ServiceLoaderTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 配置工具类
 *
 * @author zhangxt
 * @date 2023/02/07 09:58
 **/
public class ConfigurationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUtils.class);

    public static Configuration getConfiguration(String configFile) {

        Configurations configs = new Configurations();
        Configuration config = null;
        try {
            config = configs.properties(new File(configFile));
        } catch (ConfigurationException cex) {
            LOG.warn("Config File parse failed!", cex);
        }

        return config;
    }
}
