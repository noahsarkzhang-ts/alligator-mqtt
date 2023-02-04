package org.noahsark.mqtt.broker.repository.mysql;

import org.apache.ibatis.session.SqlSessionFactory;
import org.noahsark.mqtt.broker.repository.MessageRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredMessage;

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

    }

    @Override
    public StoredMessage getMessage(String topic, long offset) {
        return null;
    }

    @Override
    public List<StoredMessage> getAllMessage(String topic, long startOffset) {
        return null;
    }
}
