package io.elastic.sailor;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class AMQPWrapper implements AMQPWrapperInterface {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AMQPWrapper.class);

    private Connection amqp;
    private Channel subscribeChannel;
    private Channel publishChannel;

    private String amqpUri;
    private String subscribeExchangeName;
    private String publishExchangeName;
    private String dataRoutingKey;
    private String errorRoutingKey;
    private String reboundRoutingKey;
    private String snapshotRoutingKey;
    private CipherWrapper cipher;

    @Inject
    public AMQPWrapper(CipherWrapper cipher) {
        this.cipher = cipher;
    }

    @Inject
    public void setAmqpUri(
            @Named(ServiceSettings.ENV_VAR_AMQP_URI) String amqpUri) {
        this.amqpUri = amqpUri;
    }

    @Inject
    public void setSubscribeExchangeName(
            @Named(ServiceSettings.ENV_VAR_LISTEN_MESSAGES_ON) String subscribeExchangeName) {
        this.subscribeExchangeName = subscribeExchangeName;
    }

    @Inject
    public void setPublishExchangeName(
            @Named(ServiceSettings.ENV_VAR_PUBLISH_MESSAGES_TO) String publishExchangeName) {
        this.publishExchangeName = publishExchangeName;
    }

    @Inject
    public void setDataRoutingKey(
            @Named(ServiceSettings.ENV_VAR_DATA_ROUTING_KEY) String dataRoutingKey) {
        this.dataRoutingKey = dataRoutingKey;
    }

    @Inject
    public void setErrorRoutingKey(
            @Named(ServiceSettings.ENV_VAR_ERROR_ROUTING_KEY) String errorRoutingKey) {
        this.errorRoutingKey = errorRoutingKey;
    }

    @Inject
    public void setReboundRoutingKey(
            @Named(ServiceSettings.ENV_VAR_REBOUND_ROUTING_KEY) String reboundRoutingKey) {
        this.reboundRoutingKey = reboundRoutingKey;
    }

    @Inject
    public void setSnapshotRoutingKey(
            @Named(ServiceSettings.ENV_VAR_SNAPSHOT_ROUTING_KEY) String snapshotRoutingKey) {
        this.snapshotRoutingKey = snapshotRoutingKey;
    }

    public void connect() {
        openConnection(this.amqpUri);
        openPublishChannel();
        openSubscribeChannel();
    }

    public void disconnect() {
        logger.info("About to disconnect from AMQP");
        try {
            subscribeChannel.close();
        } catch (IOException e) {
            logger.info("Subscription channel is already closed: " + e);
        }
        try {
            publishChannel.close();
        } catch (IOException e) {
            logger.info("Publish channel is already closed: " + e);
        }
        try {
            amqp.close();
        } catch (IOException e) {
            logger.info("AMQP connection is already closed: " + e);
        }
        logger.info("Successfully disconnected from AMQP");
    }

    public void subscribeConsumer(MessageProcessor processor) {
        final MessageConsumer consumer = new MessageConsumer(subscribeChannel, cipher, processor);

        try {
            subscribeChannel.basicConsume(this.subscribeExchangeName, consumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void ack(Long deliveryTag) {
        try {
            logger.info(String.format("Message #%s ack", deliveryTag));
            subscribeChannel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reject(Long deliveryTag) {
        try {
            logger.info(String.format("Message #%s reject", deliveryTag));
            subscribeChannel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendData(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(this.dataRoutingKey, payload, options);
    }

    public void sendSnapshot(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(this.snapshotRoutingKey, payload, options);
    }

    public void sendError(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(this.errorRoutingKey, payload, options);
    }

    public void sendRebound(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(this.reboundRoutingKey, payload, options);
    }

    private AMQPWrapper openConnection(String uri) {
        try {
            if (amqp == null) {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(new URI(uri));
                amqp = factory.newConnection();
                logger.info("Connected to AMQP");
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AMQPWrapper openPublishChannel() {
        try {
            if (publishChannel == null) {
                publishChannel = amqp.createChannel();
                logger.info("Opened publish channel");
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AMQPWrapper openSubscribeChannel() {
        try {
            if (subscribeChannel == null) {
                subscribeChannel = amqp.createChannel();
                logger.info("Opened subscribe channel");
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendToExchange(String routingKey, byte[] payload, AMQP.BasicProperties options) {

        logger.info(String.format(
                "Pushing to exchange=%s, routingKey=%s, data=%s, options=%s",
                this.publishExchangeName, routingKey, new String(payload), options
        ));
        try {
            publishChannel.basicPublish(this.publishExchangeName, routingKey, options, payload);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to publish message to exchange %s, %s", subscribeExchangeName, e));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        disconnect();
        super.finalize();
    }


    public void setSubscribeChannel(Channel subscribeChannel) {
        this.subscribeChannel = subscribeChannel;
    }

    public void setPublishChannel(Channel publishChannel) {
        this.publishChannel = publishChannel;
    }
}