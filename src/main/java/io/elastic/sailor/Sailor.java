package io.elastic.sailor;

import com.google.gson.JsonObject;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Executor;
import io.elastic.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);

    private AMQPWrapperInterface amqp;
    private ComponentResolver componentResolver;
    private CipherWrapper cipher;

    public static void main(String[] args) throws IOException {
        Sailor sailor = new Sailor();
        sailor.init();
        sailor.start();
    }

    public void init() {
        componentResolver = new ComponentResolver(ServiceSettings.getComponentPath());
        cipher = new CipherWrapper(ServiceSettings.getMessageCryptoPasswort(), ServiceSettings.getMessageCryptoIV());
    }

    public void setAMQP(AMQPWrapperInterface amqp) {
        this.amqp = amqp;
    }

    public void start() throws IOException {
        logger.info("Starting up");
        amqp = new AMQPWrapper();
        amqp.connect(ServiceSettings.getAmqpUri());
        amqp.listenQueue(ServiceSettings.getListenMessagesOn(), cipher, getMessageCallback());
        logger.info("Connected to AMQP successfully");
    }

    public interface Callback{
        void receive(Message message, Map<String,Object> headers, Long deliveryTag);
    }

    private Sailor.Callback getMessageCallback(){
        return new Sailor.Callback(){
            public void receive(Message message, Map<String,Object> headers, Long deliveryTag) {
                processMessage(message, headers, deliveryTag);
            }
        };
    }

    public MessageProcessor getMessageProcessor(final ExecutionDetails executionDetails,
                                                final Message incomingMessage,
                                                final Map<String,Object> incomingHeaders,
                                                final Long deliveryTag) {
        return new MessageProcessor(
            executionDetails,
            incomingMessage,
            incomingHeaders,
            deliveryTag,
            amqp,
            cipher
        );
    }

    public void processMessage(final Message incomingMessage, final Map<String,Object> incomingHeaders, final Long deliveryTag){

        final ExecutionDetails executionDetails = new ExecutionDetails();
        final String triggerOrAction = executionDetails.getFunction();
        final String className = componentResolver.findTriggerOrAction(triggerOrAction);
        final JsonObject cfg = executionDetails.getCfg();
        final JsonObject snapshot = executionDetails.getSnapshot();

        final ExecutionParameters params = new ExecutionParameters.Builder(incomingMessage)
                .configuration(cfg)
                .snapshot(snapshot)
                .build();

        final MessageProcessor processor = getMessageProcessor(
                executionDetails, incomingMessage, incomingHeaders, deliveryTag);

        // make data callback
        EventEmitter.Callback dataCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processData(obj);
            }
        };

        // make error callback
        EventEmitter.Callback errorCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processError(obj);
            }
        };

        // make rebound callback
        EventEmitter.Callback reboundCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processRebound(obj);
            }
        };

        // snapshot callback
        EventEmitter.Callback snapshotCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processSnapshot(obj);
            }
        };

        final EventEmitter eventEmitter = new EventEmitter.Builder()
                .onData(dataCallback)
                .onError(errorCallback)
                .onRebound(reboundCallback)
                .onSnapshot(snapshotCallback)
                .build();

        final Executor executor = new Executor(className, eventEmitter);

        executor.execute(params);

        processor.processEnd();
    }
}