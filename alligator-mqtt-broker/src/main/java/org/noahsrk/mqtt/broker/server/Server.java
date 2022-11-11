package org.noahsrk.mqtt.broker.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.noahsrk.mqtt.broker.server.config.DefaultMqttSslContextFactory;
import org.noahsrk.mqtt.broker.server.config.SslContextFactory;
import org.noahsrk.mqtt.broker.server.config.BrokerConstants;
import org.noahsrk.mqtt.broker.server.handler.BytesMetricsHandler;
import org.noahsrk.mqtt.broker.server.handler.DropWizardMetricsHandler;
import org.noahsrk.mqtt.broker.server.handler.ErrorReportHandler;
import org.noahsrk.mqtt.broker.server.handler.MqttMessageLogHandler;
import org.noahsrk.mqtt.broker.server.handler.MqttEntryHandler;
import org.noahsrk.mqtt.broker.server.handler.MqttIdleTimeoutHandler;
import org.noahsrk.mqtt.broker.server.handler.MessageMetricsHandler;
import org.noahsrk.mqtt.broker.server.metric.BytesMetrics;
import org.noahsrk.mqtt.broker.server.metric.BytesMetricsCollector;
import org.noahsrk.mqtt.broker.server.metric.MessageMetrics;
import org.noahsrk.mqtt.broker.server.metric.MessageMetricsCollector;
import org.noahsrk.mqtt.broker.server.thread.EventBusThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

/**
 * MQTT server
 *
 * @author zhangxt
 * @date 2022/10/25 19:58
 **/
public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private NettyAcceptor nettyAcceptor;

    private EventBusThread eventBusThread;

    private boolean initialized = false;


    public static void main(String[] args) {
        final Server server = new Server();
        server.start();

        LOG.info("Server Started......");

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    public void start() {
        Configuration config = getConfiguration();
        start(config);
    }

    public void start(Configuration config) {

        final long startTime = System.currentTimeMillis();

        SslContextFactory sslCtxCreator = new DefaultMqttSslContextFactory(config);
        LOG.info("Using default SSL context creator");

        final MqttEntryHandler mqttHandler = new MqttEntryHandler();

        nettyAcceptor = new NettyAcceptor();
        nettyAcceptor.initialize(mqttHandler, config, sslCtxCreator);

        eventBusThread = new EventBusThread();
        eventBusThread.start();

        final long startupTime = System.currentTimeMillis() - startTime;
        LOG.info("MQTT integration has been started successfully in {} ms", startupTime);
        initialized = true;
    }

    public void stop() {
        LOG.info("Unbinding integration from the configured ports");
        nettyAcceptor.close();
        eventBusThread.shutdown();

        LOG.trace("Stopping MQTT protocol processor");
        initialized = false;
    }

    public Configuration getConfiguration() {

        Configurations configs = new Configurations();
        Configuration config = null;
        try {
            config = configs.properties(new File(BrokerConstants.DEFAULT_CONFIG));
        } catch (ConfigurationException cex) {
            LOG.warn("Config File parse failed!",cex);
        }

        return config;
    }

    private static class NettyAcceptor {

        private static final Logger LOG = LoggerFactory.getLogger(NettyAcceptor.class);

        private static final String MQTT_SUBPROTOCOL_CSV_LIST = "mqtt, mqttv3.1, mqttv3.1.1";

        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;

        private BytesMetricsCollector bytesMetricsCollector = new BytesMetricsCollector();
        private MessageMetricsCollector metricsCollector = new MessageMetricsCollector();
        private Optional<? extends ChannelInboundHandler> metrics;

        private int nettySoBacklog;
        private boolean nettySoReuseaddr;
        private boolean nettyTcpNodelay;
        private boolean nettySoKeepalive;
        private int nettyChannelTimeoutSeconds;
        private int maxBytesInMessage;

        private Class<? extends ServerSocketChannel> channelClass;

        public void initialize(MqttEntryHandler mqttHandler, Configuration props, SslContextFactory sslCtxCreator) {
            LOG.debug("Initializing Netty acceptor");

            nettySoBacklog = props.getInt(BrokerConstants.NETTY_SO_BACKLOG_PROPERTY_NAME, 128);
            nettySoReuseaddr = props.getBoolean(BrokerConstants.NETTY_SO_REUSEADDR_PROPERTY_NAME, true);
            nettyTcpNodelay = props.getBoolean(BrokerConstants.NETTY_TCP_NODELAY_PROPERTY_NAME, true);
            nettySoKeepalive = props.getBoolean(BrokerConstants.NETTY_SO_KEEPALIVE_PROPERTY_NAME, true);
            nettyChannelTimeoutSeconds = props.getInt(BrokerConstants.NETTY_CHANNEL_TIMEOUT_SECONDS_PROPERTY_NAME, 10);
            maxBytesInMessage = props.getInt(BrokerConstants.NETTY_MAX_BYTES_PROPERTY_NAME,
                    BrokerConstants.DEFAULT_NETTY_MAX_BYTES_IN_MESSAGE);

            boolean epoll = props.getBoolean(BrokerConstants.NETTY_EPOLL_PROPERTY_NAME, false);
            if (epoll) {
                LOG.info("Netty is using Epoll");
                bossGroup = new EpollEventLoopGroup();
                workerGroup = new EpollEventLoopGroup();
                channelClass = EpollServerSocketChannel.class;
            } else {
                LOG.info("Netty is using NIO");
                bossGroup = new NioEventLoopGroup();
                workerGroup = new NioEventLoopGroup();
                channelClass = NioServerSocketChannel.class;
            }

            final boolean useFineMetrics = props.getBoolean(BrokerConstants.METRICS_ENABLE_PROPERTY_NAME, false);
            if (useFineMetrics) {
                DropWizardMetricsHandler metricsHandler = new DropWizardMetricsHandler();
                metricsHandler.init(props);
                this.metrics = Optional.of(metricsHandler);
            } else {
                this.metrics = Optional.empty();
            }

            initializePlainTCPTransport(mqttHandler, props);
            initializeWebSocketTransport(mqttHandler, props);
            if (securityPortsConfigured(props)) {
                SslContext sslContext = sslCtxCreator.initSSLContext();
                if (sslContext == null) {
                    LOG.error("Can't initialize SSLHandler layer! Exiting, check your configuration of jks");
                    return;
                }
                initializeSSLTCPTransport(mqttHandler, props, sslContext);
                initializeWSSTransport(mqttHandler, props, sslContext);
            }
        }

        private boolean securityPortsConfigured(Configuration props) {
            String sslTcpPortProp = props.getString(BrokerConstants.SSL_PORT_PROPERTY_NAME);
            String wssPortProp = props.getString(BrokerConstants.WSS_PORT_PROPERTY_NAME);
            return sslTcpPortProp != null || wssPortProp != null;
        }

        private void initFactory(String host, int port, String protocol, final PipelineInitializer pipelieInitializer) {
            LOG.debug("Initializing integration. Protocol={}", protocol);
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(channelClass)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            pipelieInitializer.init(ch);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, nettySoBacklog)
                    .option(ChannelOption.SO_REUSEADDR, nettySoReuseaddr)
                    .childOption(ChannelOption.TCP_NODELAY, nettyTcpNodelay)
                    .childOption(ChannelOption.SO_KEEPALIVE, nettySoKeepalive);
            try {
                LOG.debug("Binding integration. host={}, port={}", host, port);
                // Bind and start to accept incoming connections.
                ChannelFuture f = b.bind(host, port);
                LOG.info("Server bound to host={}, port={}, protocol={}", host, port, protocol);
                f.sync().addListener(FIRE_EXCEPTION_ON_FAILURE);
            } catch (InterruptedException ex) {
                LOG.error("An interruptedException was caught while initializing integration. Protocol={}", protocol, ex);
            }
        }

        private void initializePlainTCPTransport(MqttEntryHandler mqttHandler, Configuration props) {
            LOG.debug("Configuring TCP MQTT transport");
            final MqttIdleTimeoutHandler timeoutHandler = new MqttIdleTimeoutHandler();
            String host = props.getString(BrokerConstants.HOST_PROPERTY_NAME);
            String tcpPortProp = props.getString(BrokerConstants.PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
            if (BrokerConstants.DISABLED_PORT_BIND.equals(tcpPortProp)) {
                LOG.info("Property {} has been set to {}. TCP MQTT will be disabled", BrokerConstants.PORT_PROPERTY_NAME,
                        BrokerConstants.DISABLED_PORT_BIND);
                return;
            }
            int port = Integer.parseInt(tcpPortProp);
            initFactory(host, port, "TCP MQTT", new PipelineInitializer() {

                @Override
                void init(SocketChannel channel) {
                    ChannelPipeline pipeline = channel.pipeline();
                    configureMQTTPipeline(pipeline, timeoutHandler, mqttHandler);
                }
            });
        }

        private void configureMQTTPipeline(ChannelPipeline pipeline, MqttIdleTimeoutHandler timeoutHandler,
                                           MqttEntryHandler handler) {
            pipeline.addFirst("idleStateHandler", new IdleStateHandler(nettyChannelTimeoutSeconds, 0, 0));
            pipeline.addAfter("idleStateHandler", "idleEventHandler", timeoutHandler);
            // pipeline.addLast("logger", new LoggingHandler("Netty", LogLevel.ERROR));

            pipeline.addFirst("bytemetrics", new BytesMetricsHandler(bytesMetricsCollector));
            pipeline.addLast("decoder", new MqttDecoder(maxBytesInMessage));
            pipeline.addLast("encoder", MqttEncoder.INSTANCE);
            pipeline.addLast("metrics", new MessageMetricsHandler(metricsCollector));
            pipeline.addLast("messageLogger", new MqttMessageLogHandler());
            if (metrics.isPresent()) {
                pipeline.addLast("wizardMetrics", metrics.get());
            }

            pipeline.addLast("handler", handler);
            pipeline.addLast("exceptionReport", new ErrorReportHandler());
        }

        private void initializeWebSocketTransport(final MqttEntryHandler mqttHandler, Configuration props) {
            LOG.debug("Configuring Websocket MQTT transport");
            String webSocketPortProp = props.getString(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
            if (BrokerConstants.DISABLED_PORT_BIND.equals(webSocketPortProp)) {
                // Do nothing no WebSocket configured
                LOG.info("Property {} has been setted to {}. Websocket MQTT will be disabled",
                        BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
                return;
            }
            int port = Integer.parseInt(webSocketPortProp);

            final MqttIdleTimeoutHandler timeoutHandler = new MqttIdleTimeoutHandler();

            String host = props.getString(BrokerConstants.HOST_PROPERTY_NAME);
            initFactory(host, port, "Websocket MQTT", new PipelineInitializer() {

                @Override
                void init(SocketChannel channel) {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast("httpCodec", new HttpServerCodec());
                    pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                    pipeline.addLast("webSocketHandler",
                            new WebSocketServerProtocolHandler("/mqtt", MQTT_SUBPROTOCOL_CSV_LIST));
                    pipeline.addLast("ws2bytebufDecoder", new WebSocketFrameToByteBufDecoder());
                    pipeline.addLast("bytebuf2wsEncoder", new ByteBufToWebSocketFrameEncoder());
                    configureMQTTPipeline(pipeline, timeoutHandler, mqttHandler);
                }
            });
        }

        private void initializeSSLTCPTransport(MqttEntryHandler mqttHandler, Configuration props, SslContext sslContext) {
            LOG.debug("Configuring SSL MQTT transport");
            String sslPortProp = props.getString(BrokerConstants.SSL_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
            if (BrokerConstants.DISABLED_PORT_BIND.equals(sslPortProp)) {
                // Do nothing no SSL configured
                LOG.info("Property {} has been set to {}. SSL MQTT will be disabled",
                        BrokerConstants.SSL_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
                return;
            }

            int sslPort = Integer.parseInt(sslPortProp);
            LOG.debug("Starting SSL on port {}", sslPort);

            final MqttIdleTimeoutHandler timeoutHandler = new MqttIdleTimeoutHandler();
            String host = props.getString(BrokerConstants.HOST_PROPERTY_NAME);
            String sNeedsClientAuth = props.getString(BrokerConstants.NEED_CLIENT_AUTH, "false");
            final boolean needsClientAuth = Boolean.valueOf(sNeedsClientAuth);
            initFactory(host, sslPort, "SSL MQTT", new PipelineInitializer() {

                @Override
                void init(SocketChannel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast("ssl", createSslHandler(channel, sslContext, needsClientAuth));
                    configureMQTTPipeline(pipeline, timeoutHandler, mqttHandler);
                }
            });
        }

        private void initializeWSSTransport(MqttEntryHandler mqttHandler, Configuration props, SslContext sslContext) {
            LOG.debug("Configuring secure websocket MQTT transport");
            String sslPortProp = props.getString(BrokerConstants.WSS_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
            if (BrokerConstants.DISABLED_PORT_BIND.equals(sslPortProp)) {
                // Do nothing no SSL configured
                LOG.info("Property {} has been set to {}. Secure websocket MQTT will be disabled",
                        BrokerConstants.WSS_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
                return;
            }
            int sslPort = Integer.parseInt(sslPortProp);
            final MqttIdleTimeoutHandler timeoutHandler = new MqttIdleTimeoutHandler();
            String host = props.getString(BrokerConstants.HOST_PROPERTY_NAME);
            String sNeedsClientAuth = props.getString(BrokerConstants.NEED_CLIENT_AUTH, "false");
            final boolean needsClientAuth = Boolean.valueOf(sNeedsClientAuth);
            initFactory(host, sslPort, "Secure websocket", new PipelineInitializer() {

                @Override
                void init(SocketChannel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast("ssl", createSslHandler(channel, sslContext, needsClientAuth));
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                    pipeline.addLast("webSocketHandler",
                            new WebSocketServerProtocolHandler("/mqtt", MQTT_SUBPROTOCOL_CSV_LIST));
                    pipeline.addLast("ws2bytebufDecoder", new WebSocketFrameToByteBufDecoder());
                    pipeline.addLast("bytebuf2wsEncoder", new ByteBufToWebSocketFrameEncoder());

                    configureMQTTPipeline(pipeline, timeoutHandler, mqttHandler);
                }
            });
        }

        @SuppressWarnings("FutureReturnValueIgnored")
        public void close() {
            LOG.debug("Closing Netty acceptor...");
            if (workerGroup == null || bossGroup == null) {
                LOG.error("Netty acceptor is not initialized");
                throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
            }
            Future<?> workerWaiter = workerGroup.shutdownGracefully();
            Future<?> bossWaiter = bossGroup.shutdownGracefully();

            /*
             * We shouldn't raise an IllegalStateException if we are interrupted. If we did so, the
             * broker is not shut down properly.
             */
            LOG.info("Waiting for worker and boss event loop groups to terminate...");
            try {
                workerWaiter.await(10, TimeUnit.SECONDS);
                bossWaiter.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException iex) {
                LOG.warn("An InterruptedException was caught while waiting for event loops to terminate...");
            }

            if (!workerGroup.isTerminated()) {
                LOG.warn("Forcing shutdown of worker event loop...");
                workerGroup.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
            }

            if (!bossGroup.isTerminated()) {
                LOG.warn("Forcing shutdown of boss event loop...");
                bossGroup.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
            }

            MessageMetrics metrics = metricsCollector.computeMetrics();
            BytesMetrics bytesMetrics = bytesMetricsCollector.computeMetrics();
            LOG.info("Metrics messages[read={}, write={}] bytes[read={}, write={}]", metrics.messagesRead(),
                    metrics.messagesWrote(), bytesMetrics.readBytes(), bytesMetrics.wroteBytes());

        }

        private ChannelHandler createSslHandler(SocketChannel channel, SslContext sslContext, boolean needsClientAuth) {
            SSLEngine sslEngine = sslContext.newEngine(
                    channel.alloc(),
                    channel.remoteAddress().getHostString(),
                    channel.remoteAddress().getPort());
            sslEngine.setUseClientMode(false);
            if (needsClientAuth) {
                sslEngine.setNeedClientAuth(true);
            }
            return new SslHandler(sslEngine);
        }


    }

    private static class WebSocketFrameToByteBufDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

        @Override
        protected void decode(ChannelHandlerContext chc, BinaryWebSocketFrame frame, List<Object> out)
                throws Exception {
            // convert the frame to a ByteBuf
            ByteBuf bb = frame.content();
            // System.out.println("WebSocketFrameToByteBufDecoder decode - " +
            // ByteBufUtil.hexDump(bb));
            bb.retain();
            out.add(bb);
        }
    }

    private static class ByteBufToWebSocketFrameEncoder extends MessageToMessageEncoder<ByteBuf> {

        @Override
        protected void encode(ChannelHandlerContext chc, ByteBuf bb, List<Object> out) throws Exception {
            // convert the ByteBuf to a WebSocketFrame
            BinaryWebSocketFrame result = new BinaryWebSocketFrame();
            // System.out.println("ByteBufToWebSocketFrameEncoder encode - " +
            // ByteBufUtil.hexDump(bb));
            result.content().writeBytes(bb);
            out.add(result);
        }
    }

    private abstract static class PipelineInitializer {

        abstract void init(SocketChannel channel) throws Exception;
    }

}
