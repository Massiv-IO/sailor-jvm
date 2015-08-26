package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.EventEmitter;
import io.elastic.api.Message;

import java.util.Map;

public class ReboundCallback implements EventEmitter.Callback {

    private ExecutionContext executionContext;
    private AMQPWrapperInterface amqp;
    private CipherWrapper cipher;

    public ReboundCallback(ExecutionContext executionContext, AMQPWrapperInterface amqp, CipherWrapper cipher) {
        this.executionContext = executionContext;
        this.amqp = amqp;
        this.cipher = cipher;
    }

    public void receive(Object data) {

        int reboundIteration = getReboundIteration();

        if (reboundIteration > ServiceSettings.getReboundLimit()) {
            throw new RuntimeException("Rebound limit exceeded");
        }

        final Message message = executionContext.getMessage();
        byte[] payload = cipher.encryptMessage(message).getBytes();
        Map<String, Object> headers = executionContext.buildDefaultHeaders();
        headers.put("reboundReason", data.toString());
        headers.put("reboundIteration", reboundIteration);
        double expiration = getReboundExpiration(reboundIteration);
        amqp.sendRebound(payload, makeReboundOptions(headers, expiration));
    }

    private int getReboundIteration() {
        final Map<String, Object> headers = executionContext.getHeaders();

        final Object reboundIteration = headers.get("reboundIteration");

        if (reboundIteration != null) {
            try {
                return Integer.parseInt(reboundIteration.toString()) + 1;
            } catch (Exception e) {
                throw new RuntimeException("Not a number in reboundIteration header: " + reboundIteration);
            }
        } else {
            return 1;
        }
    }

    private double getReboundExpiration(int reboundIteration) {
        return Math.pow(2, reboundIteration - 1) * ServiceSettings.getReboundInitialExpiration();
    }

    private AMQP.BasicProperties makeReboundOptions(Map<String, Object> headers, double expiration) {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .expiration(Double.toString(expiration))
                .headers(headers)
                        //TODO: .mandatory(true)
                .build();
    }
}
