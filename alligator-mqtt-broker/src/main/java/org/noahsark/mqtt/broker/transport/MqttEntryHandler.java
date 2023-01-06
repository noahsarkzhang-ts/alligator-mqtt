package org.noahsark.mqtt.broker.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import org.noahsark.mqtt.broker.transport.session.MqttConnection;
import org.noahsark.mqtt.broker.common.util.NettyUtils;
import org.noahsark.mqtt.broker.transport.session.Context;
import org.noahsark.mqtt.broker.protocol.processor.MessageProcessor;
import org.noahsark.mqtt.broker.transport.handler.InflightResenderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;

/**
 * MQTT 路由分发 Handler
 *
 * @author zhangxt
 * @date 2022/10/25 20:05
 **/
@ChannelHandler.Sharable
public class MqttEntryHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MqttEntryHandler.class);

    private Dispatcher dispatcher = new Dispatcher();

    private static void mqttConnection(Channel channel, MqttConnection connection) {
        NettyUtils.connection(channel, connection);
    }

    private static MqttConnection mqttConnection(Channel channel) {
        return NettyUtils.connection(channel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //MqttConnection connection = connectionFactory.create(ctx.channel());
        // TODO 创建 Connection 对象
        MqttConnection connection = new MqttConnection(ctx.channel());
        mqttConnection(ctx.channel(), connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final MqttConnection mqttConnection = mqttConnection(ctx.channel());
        // TODO 断线处理
        if (mqttConnection != null) {
            mqttConnection.handleConnectionLost();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        MqttMessage msg = (MqttMessage) message;
        if (msg.fixedHeader() == null) {
            throw new IOException("Unknown packet");
        }
        final MqttConnection mqttConnection = mqttConnection(ctx.channel());

        try {
            // TODO 处理数据
            MqttMessageType messageType = msg.fixedHeader().messageType();

            MessageProcessor processor = dispatcher.get(messageType);
            Context context = new Context(mqttConnection);

            if (processor == null) {
                LOG.error("Unknown MessageType: {}, channel: {}", messageType, ctx.channel());

                return;
            }

            processor.handleMessage(context, msg);
            // mqttConnection.handleMessage(msg);
        } catch (Throwable ex) {
            //ctx.fireExceptionCaught(ex);
            LOG.error("Error processing protocol message: {}", msg.fixedHeader().messageType(), ex);
            ctx.channel().close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    LOG.info("Closed client channel due to exception in processing");
                }
            });
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof InflightResenderHandler.ResendNotAckedPublishes) {
            final MqttConnection mqttConnection = mqttConnection(ctx.channel());
            mqttConnection.resendNotAckedPublishes();
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("Unexpected exception while processing MQTT message. Closing Netty channel. CId={}",
                NettyUtils.clientID(ctx.channel()), cause);
        ctx.close().addListener(CLOSE_ON_FAILURE);

        super.exceptionCaught(ctx, cause);
    }
}
