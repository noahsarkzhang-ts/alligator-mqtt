package org.noahsark.mqtt.broker.common.factory;

/**
 * 组件的生命周期组件
 *
 * @author zhangxt
 * @date 2023/01/04 16:23
 **/
public interface Lifecycle extends Initializer {

    /**
     * 启动组件
     */
    void startup();

    /**
     * 关闭组件
     */
    void shutdown();
}
