package org.noahsark.mqtt.broker.common.factory;

import org.apache.commons.configuration2.Configuration;
import org.noahsark.mqtt.broker.clusters.MqttEventBus;
import org.noahsark.mqtt.broker.clusters.MqttEventBusManager;
import org.noahsark.mqtt.broker.common.exception.OprationNotSupportedException;
import org.noahsark.mqtt.broker.protocol.subscription.SubscriptionsDirectory;
import org.noahsark.mqtt.broker.repository.RetainedRepository;
import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
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
public class MqttBeanFactory implements Lifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttBeanFactory.class);

    private static Map<Class<? extends Initializer>, Map<String, Object>> beans = new HashMap<>();

    private static Map<Class<? extends Initializer>, String> alias = new HashMap<>();

    static {
        alias.put(MqttEventBusManager.class, "singleton");
        alias.put(MqttEventBus.class, "singleton");
    }

    private Configuration conf;

    private static class Holder {
        private static final MqttBeanFactory INSTANCE = new MqttBeanFactory();
    }

    private MqttBeanFactory() {
    }

    public static MqttBeanFactory getInstance() {
        return Holder.INSTANCE;
    }

    public MqttEventBus mqttEventBus() {

        return getBean(MqttEventBus.class);
    }

    public MqttEventBusManager mqttEventBusManager() {

        return getBean(MqttEventBusManager.class);
    }

    public RetainedRepository retainedRepository() {
        return null;
    }

    public SubscriptionsRepository subscriptionsRepository() {
        return null;
    }

    public SubscriptionsDirectory subscriptionsDirectory() {
        return null;
    }

    private <T extends Initializer> T getBean(Class<T> classz) {
        T bean = (T) beans.get(classz).get(alias.get(classz));

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

            alias.put(MqttEventBusManager.class, clusterModel);
            alias.put(MqttEventBus.class, clusterModel);
        } else {
            LOG.info("cluster.model illegal:{}", clusterModel);
        }

        loadService();
    }

    private void loadService() {
        loadSevice(MqttEventBusManager.class);
        loadSevice(MqttEventBus.class);
    }

    private void loadSevice(Class<? extends Initializer> classz) {

        ServiceLoader<? extends Initializer> sl = ServiceLoader.load(classz);
        Iterator<? extends Initializer> iterator = sl.iterator();

        while (iterator.hasNext()) {
            Initializer service = iterator.next();
            String alias = service.alias();

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
