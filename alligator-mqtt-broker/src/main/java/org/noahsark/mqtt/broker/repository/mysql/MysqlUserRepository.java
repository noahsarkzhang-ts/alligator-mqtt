package org.noahsark.mqtt.broker.repository.mysql;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.noahsark.mqtt.broker.repository.UserRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredUser;
import org.noahsark.mqtt.broker.repository.mysql.mapper.StoredUserMapper;

/**
 * Mysql 版本的 Repository
 *
 * @author zhangxt
 * @date 2023/01/31 11:01
 **/
public class MysqlUserRepository implements UserRepository {

    private SqlSessionFactory sessionFactory;

    public MysqlUserRepository(SqlSessionFactory sessionFactory) {

        this.sessionFactory = sessionFactory;
    }

    @Override
    public StoredUser findUser(String username) {
        try (SqlSession session = sessionFactory.openSession()) {
            StoredUserMapper mapper = session.getMapper(StoredUserMapper.class);

            return mapper.findUser(username);
        }
    }
}
