/*
 * Copyright (c) 2012-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package org.noahsark.mqtt.broker.repository;

import org.noahsark.mqtt.broker.repository.entity.StoredSubscription;

import java.util.List;

public interface SubscriptionsRepository {

    /**
     * 获取指定 clientId 的订阅信息
     *
     * @param clientId 客户端id
     * @return 订阅关系列表
     */
    List<StoredSubscription> getAllSubscriptions(String clientId);

    /**
     * 向指定 clientId 下添加订阅关系
     *
     * @param clientId     客户端id
     * @param subscription 订阅关系
     */
    void addSubscription(String clientId, StoredSubscription subscription);

    /**
     * 移除指定clientId下的订阅关系
     *
     * @param clientId 客户端id
     * @param topic    topicFilter
     */
    void removeSubscription(String clientId, String topic);
}
