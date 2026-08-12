package com.sixro.logistics.hub.hub.infrastructure.kafka;

import com.sixro.logistics.hub.hub.application.port.HubEventPort;
import com.sixro.logistics.hub.hub.domain.event.HubStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubKafkaEventAdapter implements HubEventPort {

    private static final String TOPIC_HUB_STATUS = "hub-status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishStatusChangedEvent(HubStatusChangedEvent event) {
        kafkaTemplate.send(TOPIC_HUB_STATUS, event.hubId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Hub 상태 이벤트 발행] 성공 - HubID: {}, 변경: {} -> {}",
                                event.hubId(), event.previousStatus(), event.newStatus());
                    } else {
                        log.error("[Hub 상태 이벤트 발행] 실패 (알 수 없는 오류) - HubID: {}, 사유: {}",
                                event.hubId(), ex.getMessage(), ex);
                    }
                });
    }
}