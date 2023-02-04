package org.noahsark.mqtt.broker.repository.factory;

import org.apache.commons.configuration2.Configuration;
import org.noahsark.mqtt.broker.repository.ClientSessionRepository;
import org.noahsark.mqtt.broker.repository.InflightMessageRepository;
import org.noahsark.mqtt.broker.repository.OffsetGenerator;
import org.noahsark.mqtt.broker.repository.OffsetRepository;
import org.noahsark.mqtt.broker.repository.RetainedRepository;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.WillRepository;
import org.noahsark.mqtt.broker.repository.memory.MemoryClientSessionRepository;
import org.noahsark.mqtt.broker.repository.memory.MemoryInflightMessageRepository;
import org.noahsark.mqtt.broker.repository.memory.MemoryOffsetGenerator;
import org.noahsark.mqtt.broker.repository.memory.MemoryOffsetRepository;
import org.noahsark.mqtt.broker.repository.memory.MemoryRetainedRepository;
import org.noahsark.mqtt.broker.repository.memory.MemorySubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.memory.MemoryWillRepository;

/**
 * 内存版 Caceh Factory类
 *
 * @author zhangxt
 * @date 2023/01/31 16:13
 **/
public class MemoryCacheBeanFactory extends MemoryBeanContainer implements CacheBeanFactory {

    @Override
    public ClientSessionRepository clientSessionRepository() {
        return getBean(MemoryClientSessionRepository.class);
    }

    @Override
    public InflightMessageRepository inflightMessageRepository() {
        return getBean(MemoryInflightMessageRepository.class);
    }

    @Override
    public OffsetGenerator offsetGenerator() {
        return getBean(MemoryOffsetGenerator.class);
    }

    @Override
    public OffsetRepository offsetRepository() {
        return getBean(MemoryOffsetRepository.class);
    }

    @Override
    public RetainedRepository retainedRepository() {
        return getBean(MemoryRetainedRepository.class);
    }

    @Override
    public SubscriptionsRepository subscriptionsRepository() {
        return getBean(MemorySubscriptionsRepository.class);
    }

    @Override
    public WillRepository willRepository() {
        return getBean(MemoryWillRepository.class);
    }

    @Override
    public void init() {
    }

    @Override
    public void load(Configuration configuration) {
        LOG.info("load:{}", MemoryCacheBeanFactory.class.getSimpleName());
    }

    @Override
    public String alias() {
        return "memory";
    }

}
