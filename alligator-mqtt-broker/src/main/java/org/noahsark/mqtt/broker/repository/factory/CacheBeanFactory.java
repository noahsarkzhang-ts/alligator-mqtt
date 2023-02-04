package org.noahsark.mqtt.broker.repository.factory;

import org.noahsark.mqtt.broker.common.factory.Initializer;
import org.noahsark.mqtt.broker.repository.ClientSessionRepository;
import org.noahsark.mqtt.broker.repository.InflightMessageRepository;
import org.noahsark.mqtt.broker.repository.OffsetGenerator;
import org.noahsark.mqtt.broker.repository.OffsetRepository;
import org.noahsark.mqtt.broker.repository.RetainedRepository;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.WillRepository;

/**
 * 缓存数据对象工厂类
 *
 * @author zhangxt
 * @date 2023/01/30 17:10
 **/
public interface CacheBeanFactory extends Initializer {

    ClientSessionRepository clientSessionRepository();

    InflightMessageRepository inflightMessageRepository();

    OffsetGenerator offsetGenerator();

    OffsetRepository offsetRepository();

    RetainedRepository retainedRepository();

    SubscriptionsRepository subscriptionsRepository();

    WillRepository willRepository();

}
