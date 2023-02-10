package org.noahsark.mqtt.broker.repository.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * 内存版的对象存储类
 *
 * @author zhangxt
 * @date 2023/01/31 16:18
 **/
public class MemoryBeanContainer {

    protected static final Logger LOG = LoggerFactory.getLogger(MemoryCacheBeanFactory.class);

    /**
     * 缓存对象
     */
    protected Map<Class<?>, Object> beans = new HashMap<>();


    protected <T> T getBean(Class<T> classz) {
        T bean = (T) beans.get(classz);

        if (bean == null) {

            Constructor constructor;
            try {
                constructor = classz.getConstructor();
                bean = (T) constructor.newInstance();

                beans.put(classz, bean);

            } catch (Exception ex) {
                LOG.info("Catch an exception while load class.", ex);
            }

        }

        return bean;
    }
}
