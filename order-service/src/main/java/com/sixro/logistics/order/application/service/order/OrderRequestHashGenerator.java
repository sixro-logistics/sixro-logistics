package com.sixro.logistics.order.application.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.order.application.command.OrderCreateCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class OrderRequestHashGenerator {

    private final ObjectMapper objectMapper;

    public String generate(OrderCreateCommand command) {

        try {
            String request = objectMapper.writeValueAsString(
                    new RequestHashTarget(
                            command.hubId(),
                            command.receiverId(),
                            command.receiverCompanyId(),
                            command.deliveryDeadline(),
                            command.requests(),
                            command.orderItems()
                    )
            );

            return sha256(request);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "주문 요청 해시 생성에 실패했습니다.",
                    e
            );
        }
    }

    private String sha256(String value) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 알고리즘을 찾을 수 없습니다.",
                    e
            );
        }
    }

    private record RequestHashTarget(
            java.util.UUID hubId,
            java.util.UUID receiverId,
            java.util.UUID receiverCompanyId,
            java.time.LocalDateTime deliveryDeadline,
            String requests,
            java.util.List<?> orderItems
    ) {
    }
}