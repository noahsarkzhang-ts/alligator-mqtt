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
package org.noahsark.mqtt.broker.repository.memory;

import org.noahsark.mqtt.broker.repository.SubscriptionsRepository;
import org.noahsark.mqtt.broker.repository.entity.StoredSubscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemorySubscriptionsRepository implements SubscriptionsRepository {

    private Map<String, List<StoredSubscription>> subscriptions = new HashMap<>();

    public MemorySubscriptionsRepository() {
    }

    @Override
    public List<StoredSubscription> getAllSubscriptions(String clientId) {

        List<StoredSubscription> storedSubscriptions = subscriptions.get(clientId);
        if (storedSubscriptions != null) {
            return storedSubscriptions;
        } else {
            return new ArrayList<>();
        }

    }

    @Override
    public void addSubscription(String clientId, StoredSubscription subscription) {

        List<StoredSubscription> storedSubscriptions = subscriptions.get(clientId);
        if (storedSubscriptions != null) {
            storedSubscriptions.add(subscription);
        } else {
            storedSubscriptions = new ArrayList<>();
            storedSubscriptions.add(subscription);

            subscriptions.put(clientId, storedSubscriptions);
        }

    }

    @Override
    public void removeSubscription(String clientId, String topic) {

        List<StoredSubscription> storedSubscriptions = subscriptions.get(clientId);
        if (storedSubscriptions != null) {
            storedSubscriptions.stream()
                    .filter(s -> s.getTopicFilter().equals(topic) && s.getClientId().equals(clientId))
                    .findFirst()
                    .ifPresent(storedSubscriptions::remove);
        }

    }
}
