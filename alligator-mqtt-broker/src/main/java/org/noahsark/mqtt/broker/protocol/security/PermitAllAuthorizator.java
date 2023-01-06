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
package org.noahsark.mqtt.broker.protocol.security;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.noahsark.mqtt.broker.common.util.Utils;
import org.noahsark.mqtt.broker.protocol.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.mqtt.MqttQoS.FAILURE;

public class PermitAllAuthorizator implements Authorizator {

    private static final Logger LOG = LoggerFactory.getLogger(PermitAllAuthorizator.class);

    private final AuthorizatorPolicy policy;

    private static final class Holder {
        private static final PermitAllAuthorizator INSTANCE = new PermitAllAuthorizator();
    }

    private PermitAllAuthorizator() {
        policy = new PermitAllAuthorizatorPolicy();
    }

    public static PermitAllAuthorizator getInstance() {
        return Holder.INSTANCE;
    }


    /**
     * @param clientID the clientID
     * @param username the username
     * @param msg      the subscribe message to verify
     * @return the list of verified topics for the given subscribe message.
     */
    @Override
    public List<MqttTopicSubscription> verifyTopicsReadAccess(String clientID, String username, MqttSubscribeMessage msg) {
        List<MqttTopicSubscription> ackTopics = new ArrayList<>();

        final int messageId = Utils.messageId(msg);
        for (MqttTopicSubscription req : msg.payload().topicSubscriptions()) {
            Topic topic = new Topic(req.topicName());
            if (!policy.canRead(topic, username, clientID)) {
                // send SUBACK with 0x80, the user hasn't credentials to read the topic
                LOG.warn("Client does not have read permissions on the topic CId={}, username: {}, messageId: {}, "
                        + "topic: {}", clientID, username, messageId, topic);
                ackTopics.add(new MqttTopicSubscription(topic.toString(), FAILURE));
            } else {
                MqttQoS qos;
                if (topic.isValid()) {
                    LOG.debug("Client will be subscribed to the topic CId={}, username: {}, messageId: {}, topic: {}",
                            clientID, username, messageId, topic);
                    qos = req.qualityOfService();
                } else {
                    LOG.warn("Topic filter is not valid CId={}, username: {}, messageId: {}, topic: {}", clientID,
                            username, messageId, topic);
                    qos = FAILURE;
                }
                ackTopics.add(new MqttTopicSubscription(topic.toString(), qos));
            }
        }
        return ackTopics;
    }

    /**
     * Ask the authorization policy if the topic can be used in a publish.
     *
     * @param topic  the topic to write to.
     * @param user   the user
     * @param client the client
     * @return true if the user from client can publish data on topic.
     */
    @Override
    public boolean canWrite(Topic topic, String user, String client) {
        return policy.canWrite(topic, user, client);
    }

    @Override
    public boolean canRead(Topic topic, String user, String client) {
        return policy.canRead(topic, user, client);
    }
}
