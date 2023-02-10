package org.noahsark.mqtt.broker.repository;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.noahsark.mqtt.broker.common.ConfigurationUtils;
import org.noahsark.mqtt.broker.common.factory.MqttModuleFactory;
import org.noahsark.mqtt.broker.mybatis.StoredMessageMapperTest;
import org.noahsark.mqtt.broker.repository.entity.StoredSession;
import org.noahsark.mqtt.broker.repository.factory.CacheBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 服务加载器测试
 *
 * @author zhangxt
 * @date 2023/02/07 09:41
 **/
public class ServiceLoaderTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceLoaderTest.class);

    private static final String CONFIG_FILE = "E:\\git-repository\\alligator-mqtt\\alligator-mqtt-broker\\src\\test\\resources\\config\\alligator.properties";

    private MqttModuleFactory mqttModuleFactory;

    private CacheBeanFactory cacheBeanFactory;

    @Before
    public void setup() {

        Configuration configuration = ConfigurationUtils.getConfiguration(CONFIG_FILE);

        mqttModuleFactory = MqttModuleFactory.getInstance();

        mqttModuleFactory.load(configuration);

        cacheBeanFactory = mqttModuleFactory.cacheBeanFactory();
    }

    @Test
    public void loadRedisTest() {

        ClientSessionRepository sessionRepository = cacheBeanFactory.clientSessionRepository();

    }


}
