package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.repository.entity.StoredUser;

/**
 * 用户持 Repository 类
 *
 * @author zhangxt
 * @date 2023/01/29 15:25
 **/
public interface UserRepository {

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户数据
     */
    StoredUser findUser(String username);

}
