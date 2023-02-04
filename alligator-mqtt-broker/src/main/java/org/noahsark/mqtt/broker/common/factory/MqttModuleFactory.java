package org.noahsark.mqtt.broker.common.factory;

import org.apache.commons.configuration2.Configuration;
import org.noahsark.mqtt.broker.clusters.MqttEventBus;
import org.noahsark.mqtt.broker.clusters.MqttEventBusManager;
import org.noahsark.mqtt.broker.common.exception.OprationNotSupportedException;
import org.noahsark.mqtt.broker.repository.factory.CacheBeanFactory;
import org.noahsark.mqtt.broker.repository.factory.DbBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Mqtt 工厂类
 *
 * @author zhangxt
 * @date 2023/01/04 16:01
 **/
public class MqttModuleFactory implements Lifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttModuleFactory.class);

    private static Map<Class<? extends Initializer>, Map<String, Object>> beans = new HashMap<>();

    private static Map<Class<? extends Initializer>, String> aliasMap = new HashMap<>();

    static {
        aliasMap.put(MqttEventBusManager.class, "singleton");
        aliasMap.put(MqttEventBus.class, "singleton");
    }

    private Configuration conf;

    private static class Holder {
        private static final MqttModuleFactory INSTANCE = new MqttModuleFactory();
    }

    private MqttModuleFactory() {
    }

    public static MqttModuleFactory getInstance() {
        return Holder.INSTANCE;
    }

    public MqttEventBus mqttEventBus() {

        return getBean(MqttEventBus.class);
    }

    public MqttEventBusManager mqttEventBusManager() {

        return getBean(MqttEventBusManager.class);
    }

    public DbBeanFactory dbBeanFactory() {
        return getBean(DbBeanFactory.class);
    }

    public CacheBeanFactory cacheBeanFactory() {
        return getBean(CacheBeanFactory.class);
    }

    private <T extends Initializer> T getBean(Class<T> classz) {
        T bean = (T) beans.get(classz).get(aliasMap.get(classz));

        return bean;
    }

    @Override
    public void init() {
    }

    @Override
    public void load(Configuration configuration) {
        this.conf = configuration;

        String clusterModel = configuration.getString("cluster.model");
        if (clusterModel != null && ("cluster".equals(clusterModel) || "singleton".equals(clusterModel))) {
            LOG.info("cluster.model:{}", clusterModel);

            aliasMap.put(MqttEventBusManager.class, clusterModel);
            aliasMap.put(MqttEventBus.class, clusterModel);
        } else {
            LOG.info("cluster.model illegal:{}", clusterModel);
        }

        String cacheType = configuration.getString("cache.type");
        if (cacheType != null && ("redis".equals(cacheType) || "memory".equals(cacheType))) {
            LOG.info("cache.type:{}", cacheType);

            aliasMap.put(CacheBeanFactory.class, cacheType);
        } else {
            LOG.info("cache.type illegal:{}", cacheType);
        }

        String dbType = configuration.getString("db.type");
        if (dbType != null && ("mysql".equals(dbType) || "memory".equals(dbType))) {
            LOG.info("db.type:{}", dbType);

            aliasMap.put(DbBeanFactory.class, dbType);
        } else {
            LOG.info("db.type illegal:{}", dbType);
        }

        loadService();
    }

    private void loadService() {
        loadService(MqttEventBusManager.class);
        loadService(MqttEventBus.class);
        loadService(DbBeanFactory.class);
        loadService(CacheBeanFactory.class);
    }

    private void loadService(Class<? extends Initializer> classz) {

        ServiceLoader<? extends Initializer> sl = ServiceLoader.load(classz);
        Iterator<? extends Initializer> iterator = sl.iterator();

        while (iterator.hasNext()) {
            Initializer service = iterator.next();
            String alias = service.alias();

            // 只加载指定的对象
            if (!alias.equals(aliasMap.get(classz))) {
                continue;
            }

            service.load(conf);
            service.init();

            Map<String, Object> beanMap = beans.get(classz);
            if (beanMap == null) {
                beanMap = new HashMap<>();

                beans.put(classz, beanMap);
            }

            beanMap.put(alias, service);
        }
    }

    @Override
    public String alias() {
        return "mqtt-bean-factory";
    }

    @Override
    public void startup() {
        throw new OprationNotSupportedException();
    }

    @Override
    public void shutdown() {
        throw new OprationNotSupportedException();
    }

}
