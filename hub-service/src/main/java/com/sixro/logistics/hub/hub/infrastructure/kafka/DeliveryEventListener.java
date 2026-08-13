package com.sixro.logistics.hub.hub.infrastructure.kafka;

import com.sixro.logistics.hub.hub.application.command.HubVolumeIncreaseCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventListener {

    private final HubVolumeIncreaseCommandService hubVolumeIncreaseCommandService;
    private final StringRedisTemplate redisTemplate; // Redis 멱등성 체크용

    private static final String TOPIC_DELIVERY_STARTED = "delivery.started";
    private static final String GROUP_ID = "hub-service-group";
    private static final String IDEMPOTENCY_KEY_PREFIX = "event:processed:delivery:";

    @KafkaListener(topics = TOPIC_DELIVERY_STARTED, groupId = GROUP_ID)
    public void consumeDeliveryStartedEvent(DeliveryStartedEventMessage message) {
        log.info("[Kafka Consume] {} 이벤트 수신 - EventId: {}", TOPIC_DELIVERY_STARTED, message.eventId());

        // 멱등성 방어
        String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + message.eventId().toString();
        Boolean isFirstTime = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "DONE", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("[멱등성 방어] 이미 처리된 배송 이벤트입니다. - EventId: {}", message.eventId());
            return;
        }

        try {
            UUID originHubId = message.data().originHubId();
            int incomingVolume = message.data().getTotalVolume();

            if (incomingVolume > 0) {
                hubVolumeIncreaseCommandService.increaseAndEvaluateHubVolume(originHubId, incomingVolume);
            }
        } catch (Exception e) {
            log.error("[Kafka Consume Error] 이벤트 처리 실패 - EventId: {}", message.eventId(), e);
            redisTemplate.delete(idempotencyKey); // 실패 시 재시도를 위해 멱등성 키 삭제
            // TODO: DLQ 로직 추가
        }
    }
}