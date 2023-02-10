package org.noahsark.mqtt.broker.repository.mysql;

import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.noahsark.mqtt.broker.common.ConfigurationUtils;
import org.noahsark.mqtt.broker.common.factory.MqttModuleFactory;
import org.noahsark.mqtt.broker.repository.MessageRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredMessage;
import org.noahsark.mqtt.broker.repository.factory.DbBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mysql Repository Test
 *
 * @author zhangxt
 * @date 2023/02/07 10:00
 **/
public class MysqlMessageRepositoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(MysqlMessageRepositoryTest.class);

    private static final String CONFIG_FILE = "E:\\git-repository\\alligator-mqtt\\alligator-mqtt-broker\\src\\test\\resources\\config\\alligator.properties";

    private MqttModuleFactory mqttModuleFactory;

    private DbBeanFactory dbBeanFactory;

    private MessageRepository messageRepository;

    @Before
    public void setup() {

        Configuration configuration = ConfigurationUtils.getConfiguration(CONFIG_FILE);

        mqttModuleFactory = MqttModuleFactory.getInstance();

        mqttModuleFactory.load(configuration);

        dbBeanFactory = mqttModuleFactory.dbBeanFactory();

        messageRepository = dbBeanFactory.messageRepository();
    }

    @Test
    public void addMessage() {
        StoredMessage message = new StoredMessage();

        message.setOffset(1);
        message.setPackageId(1);
        message.setPayload("Just Test".getBytes());
        message.setQos(1);
        message.setTopic("a/b/c");

        messageRepository.addMessage(message);
    }

    @Test
    public void getMessageTest() {
        StoredMessage message = messageRepository.getMessage("a/b/c",1);

        LOG.info("message:{}",message);
    }

}
