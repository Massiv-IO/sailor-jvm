package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.JSON;

import java.util.Map;
import java.util.UUID;

public class Utils {

    public static boolean isJsonObject(final String input) {
        if (input == null) {
            return false;
        }

        try {
            JSON.parseObject(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getEnvVar(final String key) {
        final String value = getOptionalEnvVar(key);

        if (value == null) {
            throw new IllegalStateException(
                    String.format("Env var '%s' is required", key));
        }

        return value;
    }

    public static String getOptionalEnvVar(final String key) {
        String value = System.getenv(key);

        if (value == null) {
            value = System.getProperty(key);
        }

        return value;
    }

    public static AMQP.BasicProperties buildAmqpProperties(final AMQP.BasicProperties amqpProperties,
                                                           final UUID messageId,
                                                           final Map<String, Object> headers) {

        if (messageId == null) {
            throw new IllegalArgumentException("messageId is required");
        }

        return createDefaultAmqpPropertiesBuilder(headers)
                .correlationId(amqpProperties.getCorrelationId())
                .messageId(messageId.toString())
                .build();
    }

    public static AMQP.BasicProperties buildAmqpProperties(final Map<String, Object> headers) {
        return createDefaultAmqpPropertiesBuilder(headers).build();
    }

    private static AMQP.BasicProperties.Builder createDefaultAmqpPropertiesBuilder(final Map<String, Object> headers) {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .priority(1)// this should equal to mandatory true
                .deliveryMode(2);
    }

}