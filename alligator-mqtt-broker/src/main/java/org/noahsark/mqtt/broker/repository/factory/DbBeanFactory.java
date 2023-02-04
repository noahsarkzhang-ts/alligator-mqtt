package org.noahsark.mqtt.broker.repository.factory;

import org.noahsark.mqtt.broker.common.factory.Initializer;
import org.noahsark.mqtt.broker.repository.MessageRepository;
import org.noahsark.mqtt.broker.repository.UserRepository;

/**
 * 持久化对象工厂类
 *
 * @author zhangxt
 * @date 2023/01/30 17:12
 **/
public interface DbBeanFactory extends Initializer {

    UserRepository userRepository();

    MessageRepository messageRepository();
}
