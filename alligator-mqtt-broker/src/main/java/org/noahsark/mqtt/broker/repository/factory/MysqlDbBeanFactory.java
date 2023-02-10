package org.noahsark.mqtt.broker.repository.factory;

import org.apache.commons.configuration2.Configuration;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.noahsark.mqtt.broker.repository.MessageRepository;
import org.noahsark.mqtt.broker.repository.UserRepository;
import org.noahsark.mqtt.broker.repository.mysql.MysqlMessageRepository;
import org.noahsark.mqtt.broker.repository.mysql.MysqlUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * mysql 版本的持久化对象工厂类
 *
 * @author zhangxt
 * @date 2023/01/30 17:22
 **/
public class MysqlDbBeanFactory implements DbBeanFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MysqlDbBeanFactory.class);

    /**
     * 缓存对象
     */
    protected Map<Class<?>, Object> beans = new HashMap<>();

    private SqlSessionFactory sqlSessionFactory;

    private Properties properties;

    @Override
    public UserRepository userRepository() {

        return getBean(MysqlUserRepository.class);
    }

    @Override
    public MessageRepository messageRepository() {

        return getBean(MysqlMessageRepository.class);
    }

    @Override
    public void init() {
        String resource = "mybatis/mybatis-config.xml";
        InputStream inputStream;
        try {
            inputStream = Resources.getResourceAsStream(resource);

            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, properties);
        } catch (Exception ex) {
            LOG.warn("Catch an exception while init mysql db factory ", ex);
        }
    }

    @Override
    public void load(Configuration configuration) {
        // 读取配置信息
        properties = new Properties();
        properties.put("driver", configuration.getString("db.mysql.driver"));
        properties.put("url", configuration.getString("db.mysql.url"));
        properties.put("username", configuration.getString("db.mysql.username"));
        properties.put("password", configuration.getString("db.mysql.password"));

        LOG.info("load:{}", MysqlDbBeanFactory.class.getSimpleName());

    }

    @Override
    public String alias() {
        return "mysql";
    }

    private <T> T getBean(Class<T> classz) {
        T bean = (T) beans.get(classz);

        if (bean == null) {

            Constructor constructor;
            try {
                constructor = classz.getConstructor(SqlSessionFactory.class);
                bean = (T) constructor.newInstance(this.sqlSessionFactory);

                beans.put(classz, bean);

            } catch (Exception ex) {
                LOG.info("Catch an exception while load class.", ex);
            }

        }

        return bean;
    }
}
