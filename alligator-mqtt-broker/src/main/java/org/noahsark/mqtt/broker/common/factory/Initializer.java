package org.noahsark.mqtt.broker.common.factory;

import org.apache.commons.configuration2.Configuration;

/**
 * Bean 初始化器
 *
 * @author zhangxt
 * @date 2023/01/04 17:35
 **/
public interface Initializer  {

    /**
     * 初始化组件
     */
    void init();

    /**
     * 加载配置信息
     * @param configuration 配置文件
     */
    void load(Configuration configuration);

    /**
     * 组件别名
     * @return 别名
     */
    String alias();
}
