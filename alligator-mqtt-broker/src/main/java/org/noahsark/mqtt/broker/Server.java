package org.noahsark.mqtt.broker;

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
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.noahsark.mqtt.broker.clusters.MqttEventBus;
import org.noahsark.mqtt.broker.clusters.MqttEventBusManager;
import org.noahsark.mqtt.broker.common.factory.MqttModuleFactory;
import org.noahsark.mqtt.broker.transport.config.BrokerConstants;
import org.noahsark.mqtt.broker.transport.ssl.DefaultMqttSslContextFactory;
import org.noahsark.mqtt.broker.transport.ssl.SslContextFactory;
import org.noahsark.mqtt.broker.transport.handler.BytesMetricsHandler;
import org.noahsark.mqtt.broker.transport.handler.ErrorReportHandler;
import org.noahsark.mqtt.broker.transport.handler.MessageMetricsHandler;
import org.noahsark.mqtt.broker.transport.MqttEntryHandler;
import org.noahsark.mqtt.broker.transport.handler.MqttIdleTimeoutHandler;
import org.noahsark.mqtt.broker.transport.handler.MqttMessageLogHandler;
import org.noahsark.mqtt.broker.transport.metric.BytesMetrics;
import org.noahsark.mqtt.broker.transport.metric.BytesMetricsCollector;
import org.noahsark.mqtt.broker.transport.metric.MessageMetrics;
import org.noahsark.mqtt.broker.transport.metric.MessageMetricsCollector;
import org.noahsark.mqtt.broker.transport.handler.DropWizardMetricsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

/**
 * MQTT transport
 *
 * @author zhangxt
 * @date 2022/10/25 19:58
 **/
public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private NettyAcceptor nettyAcceptor;

    private Map<String, String> argsMap = new HashMap<>();

    private boolean initialized = false;

    public static void main(String[] args) {
        final Server server = new Server();
        server.start(args);

        LOG.info("Server Started......");

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    public void start(String[] args) {

        parseArgs(args);

        String configFile = argsMap.get(BrokerConstants.ARGS_CONFIG);
        Configuration config = getConfiguration(configFile);

        start(config);
    }

    public void start(Configuration config) {

        if (initialized == true) {
            LOG.info("MQTT Server had been existed!");

            return;
        }

        final long startTime = System.currentTimeMillis();

        SslContextFactory sslCtxCreator = new DefaultMqttSslContextFactory(config);
        LOG.info("Using default SSL session creator");

        loadModule(config);

        final MqttEntryHandler mqttHandler = new MqttEntryHandler();

        nettyAcceptor = new NettyAcceptor();
        nettyAcceptor.startup(mqttHandler, config, sslCtxCreator);

        // startCluster(config);
        startCluster();

        final long startupTime = System.currentTimeMillis() - startTime;
        LOG.info("MQTT integration has been started successfully in {} ms", startupTime);
        initialized = true;
    }

    private void loadModule(Configuration config) {
        MqttModuleFactory beanFactory = MqttModuleFactory.getInstance();

        beanFactory.load(config);
    }

    private void startCluster() {
        MqttEventBusManager eventBusManager = MqttModuleFactory.getInstance().mqttEventBusManager();
        eventBusManager.startup();

        MqttEventBus mqttEventBus = MqttModuleFactory.getInstance().mqttEventBus();
        mqttEventBus.startup();
    }

    public void stop() {
        LOG.info("Unbinding integration from the configured ports");
        nettyAcceptor.close();

        LOG.trace("Stopping MQTT protocol processor");
        initialized = false;
    }

    public Configuration getConfiguration(String configFile) {

        Configurations configs = new Configurations();
        Configuration config = null;
        try {
            config = configs.properties(new File(configFile));
        } catch (ConfigurationException cex) {
            LOG.warn("Config File parse failed!", cex);
        }

        return config;
    }

    private void parseArgs(String[] args) {
        Options options = new Options();

        Option opt = new Option("f", "configFile", true, "MQTT config properties file");
        opt.setRequired(false);
        options.addOption(opt);

        CommandLine commandLine = null;
        CommandLineParser parser = new DefaultParser();

        try {
            commandLine = parser.parse(options, args);

            String confName = null;
            if (commandLine.hasOption(opt)) {
                confName = commandLine.getOptionValue(opt);
            }

            if (confName == null || confName.length() == 0) {
                confName = Thread.currentThread().getContextClassLoader()
                        .getResource(BrokerConstants.DEFAULT_CONFIG).getPath();
            }

            LOG.info("Config file:{}", confName);

            argsMap.put(BrokerConstants.ARGS_CONFIG, confName);

        } catch (Exception ex) {
            LOG.warn("Parse args error.", ex);
        }
    }

    private static class NettyAcceptor {

        private static final Logger LOG = LoggerFactory.getLogger(NettyAcceptor.class);

        private static final String MQTT_SUBPROTOCOL_CSV_LIST = "mqtt, mqttv3.1, mqttv3.1.1";

        private InitializerContext initializerContext;

        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;

        private Class<? extends ServerSocketChannel> channelClass;

        private PipelineInitializerFactory pipelineInitializerFactory = new PipelineInitializerFactory();

        public void startup(MqttEntryHandler mqttHandler, Configuration props, SslContextFactory sslCtxCreator) {
            LOG.debug("Initializing Netty acceptor");

            initializerContext = new InitializerContext(props, mqttHandler);
            initializerContext.initialize();

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

            listenTcpService(props);
            listenWsService(props);
            if (initializerContext.securityPortsConfigured()) {
                SslContext sslContext = sslCtxCreator.initSSLContext();
                if (sslContext == null) {
                    LOG.error("Can't startup SSLHandler layer! Exiting, check your configuration of jks");
                    return;
                }
                listenSslTcpService(props, sslContext);
                listenWssService(props, sslContext);
            }
        }

        private void bindService(String host, int port, String protocol, final PipelineInitializer pipelieInitializer) {
            LOG.debug("Initializing integration. Protocol={}", protocol);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(channelClass)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            pipelieInitializer.init(ch);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, initializerContext.getNettySoBacklog())
                    .option(ChannelOption.SO_REUSEADDR, initializerContext.isNettySoReuseaddr())
                    .childOption(ChannelOption.TCP_NODELAY, initializerContext.isNettyTcpNodelay())
                    .childOption(ChannelOption.SO_KEEPALIVE, initializerContext.isNettySoKeepalive());
            try {
                LOG.debug("Binding integration. host={}, port={}", host, port);
                // Bind and start to accept incoming connections.
                ChannelFuture future = bootstrap.bind(host, port);
                LOG.info("Server bound to host={}, port={}, protocol={}", host, port, protocol);
                future.sync().addListener(FIRE_EXCEPTION_ON_FAILURE);
            } catch (InterruptedException ex) {
                LOG.error("An interruptedException was caught while initializing integration. Protocol={}", protocol, ex);
            }
        }

        private void listenTcpService(Configuration props) {
            LOG.debug("Configuring TCP MQTT server");

            String host = props.getString(BrokerConstants.HOST_PROPERTY_NAME);
            String tcpPortProp = props.getString(BrokerConstants.PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
            if (BrokerConstants.DISABLED_PORT_BIND.equals(tcpPortProp)) {
                LOG.info("Property {} has been set to {}. TCP MQTT will be disabled", BrokerConstants.PORT_PROPERTY_NAME,
                        BrokerConstants.DISABLED_PORT_BIND);
                return;
            }
            int port = Integer.parseInt(tcpPortProp);
            bindService(host, port, "TCP MQTT", new PipelineInitializer() {

                @Override
                void init(SocketChannel channel) {
                    initializerContext.setChannel(channel);
                    pipelineInitializerFactory.initPipeline(PipelineInitializerFactory.PipelineInitializerEnum.TCP,
                            initializerContext);
                }
            });
        }

        private void listenSslTcpService(Configuration props, SslContext sslContext) {
            LOG.debug("Configuring SSL MQTT server");
            String sslPortProp = props.getString(BrokerConstants.SSL_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
            if (BrokerConstants.DISABLED_PORT_BIND.equals(sslPortProp)) {
                // Do nothing no SSL configured
                LOG.info("Property {} has been set to {}. SSL MQTT will be disabled",
                        BrokerConstants.SSL_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
                return;
            }

            int sslPort = Integer.parseInt(sslPortProp);
            String host = props.getString(BrokerConstants.HOST_PROPERTY_NAME);

            LOG.debug("Starting SSL on port {}", sslPort);

            bindService(host, sslPort, "SSL MQTT", new PipelineInitializer() {

                @Override
                void init(SocketChannel channel) throws Exception {
                    initializerContext.setChannel(channel);
                    initializerContext.setSslContext(sslContext);
                    pipelineInitializerFactory.initPipeline(PipelineInitializerFactory.PipelineInitializerEnum.SSL,
                            initializerContext);
                }
            });
        }

        private void listenWsService(Configuration props) {
            LOG.debug("Configuring Websocket MQTT server");
            String webSocketPortProp = props.getString(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
            if (BrokerConstants.DISABLED_PORT_BIND.equals(webSocketPortProp)) {
                // Do nothing no WebSocket configured
                LOG.info("Property {} has been setted to {}. Websocket MQTT will be disabled",
                        BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
                return;
            }

            int port = Integer.parseInt(webSocketPortProp);
            String host = props.getString(BrokerConstants.HOST_PROPERTY_NAME);
            bindService(host, port, "Websocket MQTT", new PipelineInitializer() {

                @Override
                void init(SocketChannel channel) {
                    initializerContext.setChannel(channel);
                    pipelineInitializerFactory.initPipeline(PipelineInitializerFactory.PipelineInitializerEnum.WS,
                            initializerContext);
                }
            });
        }

        private void listenWssService(Configuration props, SslContext sslContext) {
            LOG.debug("Configuring secure websocket MQTT server");
            String sslPortProp = props.getString(BrokerConstants.WSS_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
            if (BrokerConstants.DISABLED_PORT_BIND.equals(sslPortProp)) {
                // Do nothing no SSL configured
                LOG.info("Property {} has been set to {}. Secure websocket MQTT will be disabled",
                        BrokerConstants.WSS_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
                return;
            }

            int sslPort = Integer.parseInt(sslPortProp);
            String host = props.getString(BrokerConstants.HOST_PROPERTY_NAME);

            bindService(host, sslPort, "Secure websocket", new PipelineInitializer() {

                @Override
                void init(SocketChannel channel) throws Exception {
                    initializerContext.setChannel(channel);
                    initializerContext.setSslContext(sslContext);
                    pipelineInitializerFactory.initPipeline(PipelineInitializerFactory.PipelineInitializerEnum.WSS,
                            initializerContext);
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

            MessageMetrics metrics = initializerContext.getMetricsCollector().computeMetrics();
            BytesMetrics bytesMetrics = initializerContext.getBytesMetricsCollector().computeMetrics();
            LOG.info("Metrics messages[read={}, write={}] bytes[read={}, write={}]", metrics.messagesRead(),
                    metrics.messagesWrote(), bytesMetrics.readBytes(), bytesMetrics.wroteBytes());

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

        public static class InitializerContext {
            private BytesMetricsCollector bytesMetricsCollector = new BytesMetricsCollector();
            private MessageMetricsCollector metricsCollector = new MessageMetricsCollector();
            private Optional<? extends ChannelInboundHandler> metrics;

            private Configuration props;

            private int nettySoBacklog;
            private boolean nettySoReuseaddr;
            private boolean nettyTcpNodelay;
            private boolean nettySoKeepalive;
            private int nettyChannelTimeoutSeconds;
            private int maxBytesInMessage;

            private MqttEntryHandler mqttHandler;
            private SslContext sslContext;
            private SocketChannel channel;


            public InitializerContext(Configuration props, MqttEntryHandler mqttHandler) {
                this.props = props;
                this.mqttHandler = mqttHandler;
            }

            public void initialize() {
                nettySoBacklog = props.getInt(BrokerConstants.NETTY_SO_BACKLOG_PROPERTY_NAME, 128);
                nettySoReuseaddr = props.getBoolean(BrokerConstants.NETTY_SO_REUSEADDR_PROPERTY_NAME, true);
                nettyTcpNodelay = props.getBoolean(BrokerConstants.NETTY_TCP_NODELAY_PROPERTY_NAME, true);
                nettySoKeepalive = props.getBoolean(BrokerConstants.NETTY_SO_KEEPALIVE_PROPERTY_NAME, true);
                nettyChannelTimeoutSeconds = props.getInt(BrokerConstants.NETTY_CHANNEL_TIMEOUT_SECONDS_PROPERTY_NAME, 10);
                maxBytesInMessage = props.getInt(BrokerConstants.NETTY_MAX_BYTES_PROPERTY_NAME,
                        BrokerConstants.DEFAULT_NETTY_MAX_BYTES_IN_MESSAGE);

                final boolean useFineMetrics = props.getBoolean(BrokerConstants.METRICS_ENABLE_PROPERTY_NAME, false);
                if (useFineMetrics) {
                    DropWizardMetricsHandler metricsHandler = new DropWizardMetricsHandler();
                    metricsHandler.init(props);
                    this.metrics = Optional.of(metricsHandler);
                } else {
                    this.metrics = Optional.empty();
                }
            }

            public boolean securityPortsConfigured() {
                String sslTcpPortProp = props.getString(BrokerConstants.SSL_PORT_PROPERTY_NAME);
                String wssPortProp = props.getString(BrokerConstants.WSS_PORT_PROPERTY_NAME);
                return sslTcpPortProp != null || wssPortProp != null;
            }

            public boolean enableSSL() {

                String sslPortProp = props.getString(BrokerConstants.SSL_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
                if (BrokerConstants.DISABLED_PORT_BIND.equals(sslPortProp)) {
                    return false;
                }

                return true;
            }

            public ChannelHandler createSslHandler() {

                String sNeedsClientAuth = props.getString(BrokerConstants.NEED_CLIENT_AUTH, "false");
                final boolean needsClientAuth = Boolean.valueOf(sNeedsClientAuth);

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

            public BytesMetricsCollector getBytesMetricsCollector() {
                return bytesMetricsCollector;
            }

            public MessageMetricsCollector getMetricsCollector() {
                return metricsCollector;
            }

            public Optional<? extends ChannelInboundHandler> getMetrics() {
                return metrics;
            }

            public int getNettySoBacklog() {
                return nettySoBacklog;
            }

            public boolean isNettySoReuseaddr() {
                return nettySoReuseaddr;
            }

            public boolean isNettyTcpNodelay() {
                return nettyTcpNodelay;
            }

            public boolean isNettySoKeepalive() {
                return nettySoKeepalive;
            }

            public int getNettyChannelTimeoutSeconds() {
                return nettyChannelTimeoutSeconds;
            }

            public int getMaxBytesInMessage() {
                return maxBytesInMessage;
            }

            public MqttEntryHandler getMqttHandler() {
                return mqttHandler;
            }

            public void setSslContext(SslContext sslContext) {
                this.sslContext = sslContext;
            }

            public void setChannel(SocketChannel channel) {
                this.channel = channel;
            }

            public SslContext getSslContext() {
                return sslContext;
            }

            public SocketChannel getChannel() {
                return channel;
            }
        }

        private static class PipelineInitializerFactory {

            public void initPipeline(PipelineInitializerEnum type, InitializerContext context) {

                ChannelPipeline pipeline = context.getChannel().pipeline();
                switch (type) {
                    case TCP: {
                        configTcpPipeline(pipeline, context);
                        break;
                    }
                    case WS: {
                        configWsPipeline(pipeline, context);
                        break;
                    }
                    case SSL: {
                        configSslPipeline(pipeline, context);
                        break;
                    }
                    case WSS: {
                        configWssPipeline(pipeline, context);
                        break;
                    }
                    default:
                        break;
                }

            }

            private void configSslPipeline(ChannelPipeline pipeline, InitializerContext context) {
                pipeline.addLast("ssl", context.createSslHandler());
                configTcpPipeline(pipeline, context);
            }

            private void configTcpPipeline(ChannelPipeline pipeline, InitializerContext context) {
                configCommonPipeline(pipeline, context);
            }

            private void configWssPipeline(ChannelPipeline pipeline, InitializerContext context) {
                pipeline.addLast("ssl", context.createSslHandler());
                configWsPipeline(pipeline, context);
            }

            private void configWsPipeline(ChannelPipeline pipeline, InitializerContext context) {

                pipeline.addLast("httpCodec", new HttpServerCodec());
                pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast("webSocketHandler",
                        new WebSocketServerProtocolHandler("/mqtt", MQTT_SUBPROTOCOL_CSV_LIST));
                pipeline.addLast("ws2bytebufDecoder", new WebSocketFrameToByteBufDecoder());
                pipeline.addLast("bytebuf2wsEncoder", new ByteBufToWebSocketFrameEncoder());

                configTcpPipeline(pipeline, context);
            }

            private void configCommonPipeline(ChannelPipeline pipeline, InitializerContext context) {
                pipeline.addFirst("idleStateHandler", new IdleStateHandler(
                        context.getNettyChannelTimeoutSeconds(), 0, 0));
                pipeline.addAfter("idleStateHandler", "idleEventHandler", new MqttIdleTimeoutHandler());
                // pipeline.addLast("logger", new LoggingHandler("Netty", LogLevel.ERROR));

                pipeline.addFirst("bytemetrics", new BytesMetricsHandler(context.getBytesMetricsCollector()));
                pipeline.addLast("decoder", new MqttDecoder(context.getMaxBytesInMessage()));
                pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                pipeline.addLast("metrics", new MessageMetricsHandler(context.getMetricsCollector()));
                pipeline.addLast("messageLogger", new MqttMessageLogHandler());
                if (context.getMetrics().isPresent()) {
                    pipeline.addLast("wizardMetrics", context.getMetrics().get());
                }

                pipeline.addLast("handler", context.getMqttHandler());
                pipeline.addLast("exceptionReport", new ErrorReportHandler());
            }

            public enum PipelineInitializerEnum {
                TCP, SSL, WS, WSS
            }

        }
    }


}
