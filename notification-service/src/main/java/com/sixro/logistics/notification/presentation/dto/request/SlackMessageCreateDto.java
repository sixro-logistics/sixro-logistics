package com.sixro.logistics.notification.presentation.dto.request;

import com.sixro.logistics.notification.domain.entity.SenderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class SlackMessageCreateDto {

    @Getter
    @NoArgsConstructor
    public static class OrderInfo {
        private Long orderId;
        private String customerName;
        private String customerEmail;
        private LocalDateTime orderTime;
        private String productName;
        private Integer quantity;
        private String requirement;
        private String origin;
        private String stopovers;
        private String destination;
        private String managerName;
        private String managerEmail;
    }

    @Getter
    @NoArgsConstructor
    public static class Request {
        private SenderType senderType;
        private UUID senderId;
        private String receiverSlackId;
        private String messageType;
        private OrderInfo orderInfo; // 주문 연동 알림 정보
    }
}
