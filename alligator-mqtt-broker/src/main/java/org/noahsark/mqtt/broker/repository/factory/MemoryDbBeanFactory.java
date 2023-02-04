package org.noahsark.mqtt.broker.repository.factory;

import org.apache.commons.configuration2.Configuration;
import org.noahsark.mqtt.broker.repository.MessageRepository;
import org.noahsark.mqtt.broker.repository.UserRepository;
import org.noahsark.mqtt.broker.repository.memory.MemoryMessageRepository;
import org.noahsark.mqtt.broker.repository.memory.MemoryUserRepository;

/**
 * 内存版 Db Factory类
 *
 * @author zhangxt
 * @date 2023/01/31 16:11
 **/
public class MemoryDbBeanFactory extends MemoryBeanContainer implements DbBeanFactory {

    @Override
    public UserRepository userRepository() {
        return getBean(MemoryUserRepository.class);
    }

    @Override
    public MessageRepository messageRepository() {
        return getBean(MemoryMessageRepository.class);
    }

    @Override
    public void init() {

    }

    @Override
    public void load(Configuration configuration) {
        LOG.info("load:{}", MemoryDbBeanFactory.class.getSimpleName());
    }

    @Override
    public String alias() {
        return "memory";
    }
}
