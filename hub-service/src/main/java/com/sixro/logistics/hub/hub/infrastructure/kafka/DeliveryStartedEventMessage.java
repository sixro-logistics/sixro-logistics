package com.sixro.logistics.hub.hub.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryStartedEventMessage(
        UUID eventId,
        DeliveryStartedData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeliveryStartedData(
            UUID originHubId,
            List<Product> products
    ) {
        // 배송된 총 박스(물동량)
        public int getTotalVolume() {
            if (products == null || products.isEmpty()) return 0;
            return products.stream()
                    .mapToInt(Product::quantity)
                    .sum();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(
            Integer quantity
    ) {}
}