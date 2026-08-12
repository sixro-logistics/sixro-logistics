package com.sixro.logistics.delivery.infrastructure.kafka.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCreatedEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserializeCommonEnvelope() throws Exception {
        String json = """
                {
                  "eventId": "c5d99fe2-7c8a-4c12-a686-a2907b39928a",
                  "occurredAt": "2026-08-11T03:10:00",
                  "data": {
                    "orderId": "75471221-0625-4548-8b34-240398ec3cdc",
                    "receiverId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
                    "hubId": "550e1234-e29b-41d4-a716-446655441234",
                    "receiverCompanyId": "2b946c7f-abd1-4aef-a440-5d7670e4db75",
                    "deliveryAddress": "배송 주소",
                    "deliveryDeadline": "2026-08-14T16:00:00",
                    "requests": "localDateTime issue complete!!",
                    "items": [
                      {
                        "companyId": "7cfd678f-2207-4bda-8fd8-c1283fa7c0b4",
                        "productId": "c9a61234-9c61-4cd9-b0eb-81c499871111",
                        "quantity": 40
                      }
                    ]
                  }
                }
                """;

        OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);

        assertThat(event.eventId()).isEqualTo(UUID.fromString("c5d99fe2-7c8a-4c12-a686-a2907b39928a"));
        assertThat(event.occurredAt()).isEqualTo(LocalDateTime.of(2026, 8, 11, 3, 10));
        assertThat(event.data().orderId()).isEqualTo(UUID.fromString("75471221-0625-4548-8b34-240398ec3cdc"));
        assertThat(event.data().deliveryDeadline()).isEqualTo(LocalDateTime.of(2026, 8, 14, 16, 0));
        assertThat(event.data().items()).singleElement().satisfies(item -> {
            assertThat(item.companyId()).isEqualTo(UUID.fromString("7cfd678f-2207-4bda-8fd8-c1283fa7c0b4"));
            assertThat(item.productId()).isEqualTo(UUID.fromString("c9a61234-9c61-4cd9-b0eb-81c499871111"));
            assertThat(item.quantity()).isEqualTo(40);
        });
    }
}
