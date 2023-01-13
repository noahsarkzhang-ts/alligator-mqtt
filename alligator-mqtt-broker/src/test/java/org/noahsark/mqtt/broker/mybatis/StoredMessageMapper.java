package org.noahsark.mqtt.broker.mybatis;

import org.apache.ibatis.annotations.Param;
import org.noahsark.mqtt.broker.protocol.entity.StoredMessage;

/**
 * StoredMessage Mapper
 *
 * @author zhangxt
 * @date 2023/01/11 09:53
 **/
public interface StoredMessageMapper {

    StoredMessage getMessageById(@Param("id") Integer id);

    int store(StoredMessage msg);
}
