package org.noahsark.mqtt.broker.repository.mysql.mapper;

import org.apache.ibatis.annotations.Param;
import org.noahsark.mqtt.broker.repository.entity.StoredMessage;

import java.util.List;

/**
 * StoredMessage Mapper
 *
 * @author zhangxt
 * @date 2023/01/11 09:53
 **/
public interface StoredMessageMapper {

    StoredMessage getMessage(@Param("topic") String topic, @Param("offset") long offset);

    void addMessage(StoredMessage msg);

    List<StoredMessage> getAllMessage(@Param("topic") String topic, @Param("offset") long startOffset);
}
