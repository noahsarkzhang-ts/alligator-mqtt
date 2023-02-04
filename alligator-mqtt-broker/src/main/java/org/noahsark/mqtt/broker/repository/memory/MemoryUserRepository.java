package org.noahsark.mqtt.broker.repository.memory;

import org.noahsark.mqtt.broker.repository.UserRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredUser;

/**
 * 内存版 User Repository类
 *
 * @author zhangxt
 * @date 2023/01/31 15:57
 **/
public class MemoryUserRepository implements UserRepository {


    @Override
    public StoredUser findUser(String username) {
        return null;
    }
}
