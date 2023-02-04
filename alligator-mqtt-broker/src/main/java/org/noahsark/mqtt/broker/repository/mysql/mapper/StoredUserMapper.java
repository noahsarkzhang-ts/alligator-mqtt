package org.noahsark.mqtt.broker.repository.mysql.mapper;

import org.apache.ibatis.annotations.Param;
import org.noahsark.mqtt.broker.repository.entity.StoredMessage;
import org.noahsark.mqtt.broker.repository.entity.StoredUser;

/**
 * StoredMessage Mapper
 *
 * @author zhangxt
 * @date 2023/01/11 09:53
 **/
public interface StoredUserMapper {

    StoredUser findUser(@Param("username") String username);

}
