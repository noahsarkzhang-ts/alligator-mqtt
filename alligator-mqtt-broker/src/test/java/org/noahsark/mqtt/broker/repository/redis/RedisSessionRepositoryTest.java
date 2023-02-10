package org.noahsark.mqtt.broker.repository.redis;

import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.noahsark.mqtt.broker.common.ConfigurationUtils;
import org.noahsark.mqtt.broker.common.factory.MqttModuleFactory;
import org.noahsark.mqtt.broker.repository.ClientSessionRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredSession;
import org.noahsark.mqtt.broker.repository.factory.CacheBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis Repository Test
 *
 * @author zhangxt
 * @date 2023/02/07 10:01
 **/
public class RedisSessionRepositoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(RedisSessionRepositoryTest.class);

    private static final String CONFIG_FILE = "E:\\git-repository\\alligator-mqtt\\alligator-mqtt-broker\\src\\main\\resources\\config\\alligator.properties";

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
    public void addSessionTest() {
        ClientSessionRepository sessionRepository = cacheBeanFactory.clientSessionRepository();

        StoredSession session = new StoredSession();
        session.setStatus(1);
        session.setServerId(1);
        session.setClean(false);
        session.setUserName("allen");
        session.setClientId("mqtt_client_1");
        session.setTimestamp(System.currentTimeMillis());

        sessionRepository.addSession(session.getClientId(), session);
    }

    @Test
    public void getSessionTest() {
        ClientSessionRepository sessionRepository = cacheBeanFactory.clientSessionRepository();

        String clientId = "mqtt_client_1";

        StoredSession storedSession = sessionRepository.getSession(clientId);

        LOG.info("session:{}", storedSession);
    }
}
