package org.noahsrk.mqtt.broker.server.context;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateHandler;
import org.noahsrk.mqtt.broker.server.common.DebugUtils;
import org.noahsrk.mqtt.broker.server.common.NettyUtils;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

/**
 * MQTT connection
 *
 * @author zhangxt
 * @date 2022/10/25 20:03
 **/
public class MqttConnection {

    private static final Logger LOG = LoggerFactory.getLogger(MqttConnection.class);

    private final AtomicInteger lastPacketId = new AtomicInteger(0);

    private Channel channel;

    private boolean connected = false;

    private SessionManager sessionManager = SessionManager.getInstance();

    public MqttConnection(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public int nextPacketId() {
        return lastPacketId.incrementAndGet();
    }

    public boolean isNotProtocolVersion(MqttConnectMessage msg, MqttVersion version) {
        return msg.variableHeader().version() != version.protocolLevel();
    }

    public void abortConnection(MqttConnectReturnCode returnCode) {
        MqttConnAckMessage badProto = connAck(returnCode, false);
        channel.writeAndFlush(badProto).addListener(FIRE_EXCEPTION_ON_FAILURE);
        channel.close().addListener(CLOSE_ON_FAILURE);
    }

    public MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    public void sendConnAck(boolean isSessionAlreadyPresent) {
        connected = true;
        final MqttConnAckMessage ackMessage = connAck(CONNECTION_ACCEPTED, isSessionAlreadyPresent);
        channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    public void initializeKeepAliveTimeout(MqttConnectMessage msg, String clientId) {
        int keepAlive = msg.variableHeader().keepAliveTimeSeconds();
        NettyUtils.keepAlive(channel, keepAlive);
        NettyUtils.cleanSession(channel, msg.variableHeader().isCleanSession());
        NettyUtils.clientID(channel, clientId);
        int idleTime = Math.round(keepAlive * 1.5f);
        setIdleTime(channel.pipeline(), idleTime);

        LOG.debug("Connection has been configured CId={}, keepAlive={}, removeTemporaryQoS2={}, idleTime={}",
                clientId, keepAlive, msg.variableHeader().isCleanSession(), idleTime);
    }

    private void setIdleTime(ChannelPipeline pipeline, int idleTime) {
        if (pipeline.names().contains("idleStateHandler")) {
            pipeline.remove("idleStateHandler");
        }
        pipeline.addFirst("idleStateHandler", new IdleStateHandler(idleTime, 0, 0));
    }

    public void processDisconnect(MqttMessage msg) {
        final String clientID = NettyUtils.clientID(channel);
        LOG.trace("Start DISCONNECT CId={}, channel: {}", clientID, channel);
        if (!connected) {
            LOG.info("DISCONNECT received on already closed connection, CId={}, channel: {}", clientID, channel);
            return;
        }

        // TODO 清除会话信息
        sessionManager.disconnect(clientID);
        connected = false;
        channel.close().addListener(FIRE_EXCEPTION_ON_FAILURE);
        LOG.trace("Processed DISCONNECT CId={}, channel: {}", clientID, channel);
    }

    public void sendSubAckMessage(int messageID, MqttSubAckMessage ackMessage) {
        final String clientId = NettyUtils.clientID(channel);
        LOG.trace("Sending SUBACK response CId={}, messageId: {}", clientId, messageID);
        channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    public void sendUnsubAckMessage(List<String> topics, String clientID, int messageID) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, AT_MOST_ONCE,
                false, 0);
        MqttUnsubAckMessage ackMessage = new MqttUnsubAckMessage(fixedHeader, from(messageID));

        LOG.trace("Sending UNSUBACK message. CId={}, messageId: {}, topics: {}", clientID, messageID, topics);
        channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
        LOG.trace("Client <{}> unsubscribed from topics <{}>", clientID, topics);
    }

    public void sendPublishNotRetainedQos0(Topic topic, MqttQoS qos, ByteBuf payload) {
        MqttPublishMessage publishMsg = notRetainedPublish(topic.toString(), qos, payload);
        sendPublish(publishMsg);
    }

    private MqttPublishMessage notRetainedPublish(String topic, MqttQoS qos, ByteBuf message) {
        return notRetainedPublishWithMessageId(topic, qos, message, 0);
    }

    private MqttPublishMessage notRetainedPublishWithMessageId(String topic, MqttQoS qos, ByteBuf message,
                                                               int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, false, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, messageId);
        return new MqttPublishMessage(fixedHeader, varHeader, message);
    }

    public void sendPublish(MqttPublishMessage publishMsg) {
        final int packetId = publishMsg.variableHeader().packetId();
        final String topicName = publishMsg.variableHeader().topicName();
        final String clientId = getClientId();
        MqttQoS qos = publishMsg.fixedHeader().qosLevel();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending PUBLISH({}) message. MessageId={}, CId={}, topic={}, payload={}", qos, packetId,
                    clientId, topicName, DebugUtils.payload2Str(publishMsg.payload()));
        } else {
            LOG.debug("Sending PUBLISH({}) message. MessageId={}, CId={}, topic={}", qos, packetId, clientId,
                    topicName);
        }
        sendIfWritableElseDrop(publishMsg);
    }

    public void sendIfWritableElseDrop(MqttMessage msg) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("OUT {} on channel {}", msg.fixedHeader().messageType(), channel);
        }
        if (channel.isWritable()) {
            channel.write(msg).addListener(FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    public void sendPublishReceived(int messageID) {
        LOG.trace("sendPubRec invoked on channel: {}", channel);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE,
                false, 0);
        MqttPubAckMessage pubRecMessage = new MqttPubAckMessage(fixedHeader, from(messageID));
        sendIfWritableElseDrop(pubRecMessage);
    }

    public String getClientId() {
        return NettyUtils.clientID(channel);
    }

    public void dropConnection() {
        channel.close().addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
