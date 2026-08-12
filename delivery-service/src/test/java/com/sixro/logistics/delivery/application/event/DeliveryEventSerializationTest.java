package com.sixro.logistics.delivery.application.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryEventSerializationTest {

    private static final LocalDateTime OCCURRED_AT =
            LocalDateTime.of(2026, 8, 12, 13, 48, 28, 538_241_100);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("배송 생성 실패 이벤트의 occurredAt은 ISO-8601 문자열로 직렬화한다")
    void deliveryCreationFailedEvent_serializesOccurredAtAsIsoString() throws Exception {
        DeliveryCreationFailedEvent event = new DeliveryCreationFailedEvent(
                UUID.randomUUID(),
                OCCURRED_AT,
                new DeliveryCreationFailedEvent.DeliveryCreationFailedData(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        DeliveryCreationKafkaErrorCode.RECEIVER_NOT_FOUND,
                        DeliveryCreationKafkaErrorCode.RECEIVER_NOT_FOUND.getMessage()
                )
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(event));

        assertThat(json.path("occurredAt").isTextual()).isTrue();
        assertThat(json.path("occurredAt").asText()).isEqualTo("2026-08-12T13:48:28");
    }

    @Test
    @DisplayName("배송 생성 완료 이벤트의 occurredAt도 ISO-8601 문자열로 직렬화한다")
    void deliveryCreatedEvent_serializesOccurredAtAsIsoString() throws Exception {
        DeliveryCreatedEvent event = new DeliveryCreatedEvent(
                UUID.randomUUID(),
                OCCURRED_AT,
                null
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(event));

        assertThat(json.path("occurredAt").isTextual()).isTrue();
        assertThat(json.path("occurredAt").asText()).isEqualTo("2026-08-12T13:48:28");
    }
}
