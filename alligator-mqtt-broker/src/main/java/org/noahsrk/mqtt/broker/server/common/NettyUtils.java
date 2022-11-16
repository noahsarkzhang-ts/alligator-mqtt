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

package org.noahsrk.mqtt.broker.server.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.noahsrk.mqtt.broker.server.context.MqttConnection;

/**
 * Some Netty's channels utilities.
 */
public final class NettyUtils {

    public static Object getAttribute(ChannelHandlerContext ctx, AttributeKey<Object> key) {
        Attribute<Object> attr = ctx.channel().attr(key);
        return attr.get();
    }

    public static void keepAlive(Channel channel, int keepAlive) {
        channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_KEEPALIVE).set(keepAlive);
    }

    public static void cleanSession(Channel channel, boolean cleanSession) {
        channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_CLEANSESSION).set(cleanSession);
    }

    public static boolean cleanSession(Channel channel) {
        return (Boolean) channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_CLEANSESSION).get();
    }

    public static void clientID(Channel channel, String clientID) {
        channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_CLIENTID).set(clientID);
    }

    public static String clientID(Channel channel) {
        return (String) channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_CLIENTID).get();
    }

    public static void userName(Channel channel, String username) {
        channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_USERNAME).set(username);
    }

    public static String userName(Channel channel) {
        return (String) channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_USERNAME).get();
    }

    public static void connection(Channel channel, MqttConnection connection) {
        channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_CONNECTION).set(connection);
    }

    public static MqttConnection connection(Channel channel) {
        return (MqttConnection) channel.attr(MqttConnection.ConnectionAttr.ATTR_KEY_CONNECTION).get();
    }

    private NettyUtils() {
    }
}
