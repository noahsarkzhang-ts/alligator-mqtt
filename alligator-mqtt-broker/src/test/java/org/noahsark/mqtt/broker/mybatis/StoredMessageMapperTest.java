package org.noahsark.mqtt.broker.mybatis;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.noahsark.mqtt.broker.repository.entity.StoredMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * StoredMessageMapper Test
 *
 * @author zhangxt
 * @date 2023/01/11 10:12
 **/
public class StoredMessageMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(StoredMessageMapperTest.class);

    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void setup() {
        String resource = "org/noahsark/mqtt/broker/mybatis/mybatis-config.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Test
    public void storeTest() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            StoredMessageMapper mapper = session.getMapper(StoredMessageMapper.class);

            StoredMessage message = new StoredMessage();
            message.setOffset(1);
            message.setPayload("Hello MQTT2".getBytes());
            message.setQos(1);
            message.setTopic("a/b/c");

            int id = mapper.store(message);

            session.commit();

            LOG.info("message Id:{}",id);
        }
    }

    @Test
    public void getMessageByIdTest() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            StoredMessageMapper mapper = session.getMapper(StoredMessageMapper.class);

            int id = 4;

            StoredMessage msg = mapper.getMessageById(id);

            LOG.info("topic:{},qos:{},payload:{}",msg.getTopic(),msg.getQos(),new String(msg.getPayload()));

        }
    }
}
