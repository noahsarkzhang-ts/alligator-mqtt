package org.noahsark.mqtt.broker.repository.mysql;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.noahsark.mqtt.broker.repository.MessageRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredMessage;
import org.noahsark.mqtt.broker.repository.mysql.mapper.StoredMessageMapper;

import java.util.List;

/**
 * Mysql 版本的 MessageRepository
 *
 * @author zhangxt
 * @date 2022/11/25 14:23
 **/
public class MysqlMessageRepository implements MessageRepository {

    private SqlSessionFactory sessionFactory;

    public MysqlMessageRepository(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }


    @Override
    public void addMessage(StoredMessage msg) {
        try (SqlSession session = sessionFactory.openSession()) {
            StoredMessageMapper mapper = session.getMapper(StoredMessageMapper.class);

            mapper.addMessage(msg);

            session.commit();
        }

    }

    @Override
    public StoredMessage getMessage(String topic, long offset) {

        try (SqlSession session = sessionFactory.openSession()) {
            StoredMessageMapper mapper = session.getMapper(StoredMessageMapper.class);

            return mapper.getMessage(topic, offset);
        }

    }

    @Override
    public List<StoredMessage> getAllMessage(String topic, long startOffset) {
        try (SqlSession session = sessionFactory.openSession()) {
            StoredMessageMapper mapper = session.getMapper(StoredMessageMapper.class);

            return mapper.getAllMessage(topic, startOffset);
        }
    }
}
