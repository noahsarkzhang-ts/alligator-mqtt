package org.noahsrk.mqtt.broker.server.clusters;

import org.noahsark.rpc.common.constant.SerializerType;
import org.noahsark.rpc.common.remote.CommandCallback;
import org.noahsark.rpc.common.remote.Request;
import org.noahsark.rpc.socket.session.Session;
import org.noahsrk.mqtt.broker.server.clusters.bean.ClusterMessage;
import org.noahsrk.mqtt.broker.server.clusters.bean.ClusterPublishInnerInfo;
import org.noahsrk.mqtt.broker.server.clusters.bean.ClusterSubscriptionInfo;
import org.noahsrk.mqtt.broker.server.core.AbstractMqttEventBus;
import org.noahsrk.mqtt.broker.server.core.bean.PublishInnerMessage;
import org.noahsrk.mqtt.broker.server.subscription.Token;
import org.noahsrk.mqtt.broker.server.subscription.Topic;
import org.noahsrk.mqtt.broker.server.thread.ServiceThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 集群版的 EventBus
 *
 * @author zhangxt
 * @date 2022/12/07 10:02
 **/
public class ClustersEventBus extends AbstractMqttEventBus {

    private BlockingQueue<ClusterMessage> sending;

    private BlockingQueue<ClusterMessage> receive;

    private MessageProcessThread sendingThread;

    private MessageProcessThread receiveThread;

    private static final class Holder {
        private static final ClustersEventBus INSTANCE = new ClustersEventBus();
    }

    public static ClustersEventBus getInstance() {
        return Holder.INSTANCE;
    }

    private ClustersEventBus() {
        sending = new LinkedBlockingQueue<>();
        receive = new LinkedBlockingQueue<>();

        sendingThread = new MessageProcessThread("sending", sending, this::processBroadcast);
        receiveThread = new MessageProcessThread("receive", receive, this::processReceive);

        sendingThread.start();
        receiveThread.start();

    }

    @Override
    public void broadcast(ClusterMessage msg) {
        // TODO
        sending.offer(msg);
    }

    @Override
    public void receive(ClusterMessage msg) {
        // TODO
        receive.offer(msg);
        //publish2Subscribers(msg);
    }

    private void processBroadcast(ClusterMessage msg) {
        switch (msg.getMessageType()) {
            case PUBLISH: {
                PublishInnerMessage publish = (PublishInnerMessage) msg.getMessage();
                processPublishBroadcast(publish);
                break;
            }
            case SUBSCRIPTION: {
                ClusterSubscriptionInfo subscriptionInfo = (ClusterSubscriptionInfo) msg.getMessage();
                processSubscriptionBroadcast(subscriptionInfo);

                break;
            }
            default: {
                break;
            }

        }
    }

    private void processPublishBroadcast(PublishInnerMessage msg) {

        Topic topic = msg.getTopic();
        Token token = topic.headToken();

        final Map<String, Set<Integer>> topicHolders = MqttClusterGrid.getInstance().getTopicHolders();

        Set<Integer> serverSet = topicHolders.get(token.toString());
        if(serverSet == null || serverSet.isEmpty()) {
            // 向本结点发送数据
            publish2Subscribers(msg);

            return;
        }

        Map<Integer, Session> sessionMap = MqttClusterGrid.getInstance().traverseSessions();
        Set<Session> sessions = new HashSet<>();

        for (Integer serverId : serverSet) {
            sessions.add(sessionMap.get(serverId));
        }

        sessions.forEach(session -> {
            // 向集群广播消息
            // 构造 Request.

            Request request = buildPublishRequest(msg);
            // session.invoke

            session.invoke(request, new CommandCallback() {
                @Override
                public void callback(Object result, int currentFanout, int fanout) {
                    LOG.info("Send message successfully:{},{}", session.getSubject().getId(), msg);
                }

                @Override
                public void failure(Throwable cause, int currentFanout, int fanout) {
                    LOG.info("Send message failed:{},{}", session.getSubject().getId(), msg);
                }

            }, 300000);
        });

        // 向本结点发送数据
        publish2Subscribers(msg);
    }

    private void processSubscriptionBroadcast(ClusterSubscriptionInfo msg) {
        final Map<Integer, Session> sessionMap = MqttClusterGrid.getInstance().traverseSessions();
        final Collection<Session> sessions = sessionMap.values();

        sessions.forEach(session -> {
            // 向集群广播消息
            // 构造 Request.
            Request request = buildSubscriptionRequest(msg);

            session.invoke(request, new CommandCallback() {
                @Override
                public void callback(Object result, int currentFanout, int fanout) {
                    LOG.info("Send subscription message successfully:{},{}", session.getSubject().getId(), msg);
                }

                @Override
                public void failure(Throwable cause, int currentFanout, int fanout) {
                    LOG.info("Send subscription message failed:{},{}", session.getSubject().getId(), msg);
                }

            }, 300000);
        });

    }

    private ClusterPublishInnerInfo copyClusterInfo(PublishInnerMessage msg) {
        ClusterPublishInnerInfo clusterPublishInnerInfo = new ClusterPublishInnerInfo();

        clusterPublishInnerInfo.setMessageId(msg.getMessageId());
        clusterPublishInnerInfo.setPayload(msg.getPayload());
        clusterPublishInnerInfo.setQos(msg.getQos());
        clusterPublishInnerInfo.setRetain(msg.isRetain());
        clusterPublishInnerInfo.setTopic(msg.getTopic().getRawTopic());

        return clusterPublishInnerInfo;
    }

    private Request buildPublishRequest(PublishInnerMessage msg) {
        ClusterPublishInnerInfo clusterPublishInnerInfo = copyClusterInfo(msg);

        return buildRequest(5, 101, clusterPublishInnerInfo);
    }

    private Request buildSubscriptionRequest(ClusterSubscriptionInfo msg) {

        return buildRequest(5, 102, msg);
    }

    private Request buildRequest(int biz, int cmd, Object payload) {

        Request request = new Request.Builder()
                .biz(biz)
                .cmd(cmd)
                .serializer(SerializerType.PROBUFFER)
                .payload(payload)
                .build();

        return request;
    }

    private void processReceive(ClusterMessage msg) {
        switch (msg.getMessageType()) {
            case PUBLISH: {
                PublishInnerMessage publish = (PublishInnerMessage) msg.getMessage();
                publish2Subscribers(publish);
                break;
            }
            case SUBSCRIPTION: {
                // TODO
                ClusterSubscriptionInfo subscriptionInfo = (ClusterSubscriptionInfo) msg.getMessage();
                processSubscriptionReceive(subscriptionInfo);
                break;
            }
            default: {
                // TODO
                break;
            }

        }

    }

    private void processSubscriptionReceive(ClusterSubscriptionInfo msg) {
        MqttClusterGrid.getInstance().subscription(msg);
    }

    private static class MessageProcessThread extends ServiceThread {

        private static final Logger LOG = LoggerFactory.getLogger(MessageProcessThread.class);

        /**
         * 超时时间
         */
        private static final int TIMEOUT_MS = 60 * 1000;

        private BlockingQueue<ClusterMessage> messages;

        private Consumer<ClusterMessage> processor;

        private String type;

        public MessageProcessThread(String type, BlockingQueue<ClusterMessage> messages, Consumer<ClusterMessage> processor) {
            this.type = type;
            this.messages = messages;
            this.processor = processor;
        }

        @Override
        public void run() {
            LOG.info(getServiceName() + " Thread start in {}", LocalDateTime.now());

            ClusterMessage message;

            while (!this.isStopped()) {
                try {

                    message = messages.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);

                    if (message != null) {
                        processor.accept(message);
                    }

                } catch (Exception ex) {
                    LOG.warn("catch an exception!", ex);
                }
            }

            LOG.info(" {} Thread stop in {}", this.getServiceName(), LocalDateTime.now());
        }

        @Override
        public String getServiceName() {
            return "clusters-message-" + type;
        }
    }
}
