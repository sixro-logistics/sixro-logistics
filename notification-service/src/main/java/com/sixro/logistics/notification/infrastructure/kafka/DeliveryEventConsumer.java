package com.sixro.logistics.notification.infrastructure.kafka;


import com.sixro.logistics.notification.application.service.AiService;
import com.sixro.logistics.notification.application.service.SlackService;
import com.sixro.logistics.notification.domain.entity.SenderType;
import com.sixro.logistics.notification.domain.event.DeliveryCreatedEvent;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventConsumer {

    private final StringRedisTemplate redisTemplate;
    private final AiService aiService;
    private final SlackService slackService;

    // 시스템 자동 발송을 위한 공통 식별자
    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @KafkaListener(topics = "delivery.events", groupId = "notification-service-group")
    public void consumeDeliveryCreatedEvent(
            @Payload DeliveryCreatedEvent event,
            @Header(value = "event-type", required = false) String eventType,
            @Header(value = "trace-id", required = false) String traceId
    ) {
        String eventId = event.getEventId().toString();
        log.info("[Kafka Consumer] Received event. eventId: {}, eventType: {}, traceId: {}", eventId, eventType, traceId);

        Boolean isFirstProcessed = redisTemplate.opsForValue()
                .setIfAbsent("processed_event:" + eventId, "TRUE", Duration.ofDays(7));

        if (Boolean.FALSE.equals(isFirstProcessed)) {
            log.warn("[Kafka Consumer] Duplicate event detected. Skipping eventId: {}", eventId);
            return;
        }

        try {
            processNotification(event);
        } catch (Exception e) {
            redisTemplate.delete("processed_event:" + eventId);
            log.error("[Kafka Consumer] Error processing eventId: {}. Removed idempotency key.", eventId, e);
            throw e;
        }
    }

    private void processNotification(DeliveryCreatedEvent event) {
        DeliveryCreatedEvent.DeliveryCreatedData data = event.getData();

        // 1. AI 메시지 템플릿 생성
        String generatedMessage = aiService.generateSlackMessage(data);

        // 2. 대상자 추출 및 SlackService 호출
        List<DeliveryCreatedEvent.DeliveryManagerInfo> managers = data.getDeliveryManagers();

        if (CollectionUtils.isEmpty(managers)) {
            // 미배정 상태일 경우: 사전에 정의된 관리자(Admin) 채널/사용자에게 알림
            String adminSlackId = "U0BPCKLQ3PC"; // 실제 관리자 Slack ID 또는 채널명으로 교체 (현재는 테스트용)
            sendViaSlackService(adminSlackId, generatedMessage);
        } else {
            // 배정 완료 상태: 각 담당자별로 동적 발송
            for (DeliveryCreatedEvent.DeliveryManagerInfo manager : managers) {
                String receiverSlackId = manager.getDeliveryManagerId() != null
                        ? manager.getDeliveryManagerId().toString()
                        : "UNKNOWN_RECEIVER";

                sendViaSlackService(receiverSlackId, generatedMessage);
            }
        }
    }

    private void sendViaSlackService(String receiverSlackId, String messageContent) {
        try {
            // DTO 필드 순서에 맞게 생성자 호출
            SlackMessageSendRequest request = new SlackMessageSendRequest(
                    SenderType.SYSTEM,
                    SYSTEM_USER_ID,
                    "SYSTEM_BOT",
                    receiverSlackId,
                    messageContent,
                    "DELIVERY_CREATED" // messageType
            );

            // SlackService의 sendMessage 메서드 활용 (DB 저장 + API 호출 + 상태 업데이트 자동 수행)
            slackService.sendMessage(request, SYSTEM_USER_ID);

            log.info("[Slack Delivery Success] Routed message to receiverSlackId: {}", receiverSlackId);
        } catch (Exception e) {
            log.error("[Slack Delivery Failed] Failed to send to receiverSlackId: {}. Reason: {}", receiverSlackId, e.getMessage(), e);
        }
    }
}
