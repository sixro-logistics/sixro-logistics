package com.sixro.logistics.hub.hub.presentation.api;

import com.sixro.logistics.hub.hub.infrastructure.kafka.DeliveryStartedEventMessage;
import com.sixro.logistics.hub.hub.presentation.dto.MockVolumeRequest;
import com.sixro.logistics.hub.hub.presentation.dto.MockVolumeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/mock/hubs")
@RequiredArgsConstructor
public class HubMockController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_DELIVERY_STARTED = "delivery.started";

    // 테스트용 simple API
    @PostMapping("/volume")
    public ResponseEntity<MockVolumeResponse> injectMockVolume(@RequestBody MockVolumeRequest request) {
        log.info("[Mock API] 허브({}) 물동량 수동 주입 요청: {} 박스", request.hubId(), request.totalVolume());

        UUID eventId = UUID.randomUUID();

        DeliveryStartedEventMessage.Product dummyProduct =
                new DeliveryStartedEventMessage.Product(request.totalVolume());

        DeliveryStartedEventMessage.DeliveryStartedData dummyData =
                new DeliveryStartedEventMessage.DeliveryStartedData(request.hubId(), List.of(dummyProduct));

        DeliveryStartedEventMessage message = new DeliveryStartedEventMessage(eventId, dummyData);

        kafkaTemplate.send(TOPIC_DELIVERY_STARTED, eventId.toString(), message);

        MockVolumeResponse response = new MockVolumeResponse(
                eventId,
                request.hubId(),
                request.totalVolume(),
                "가상 물동량 주입 이벤트가 카프카에 성공적으로 발행되었습니다."
        );

        return ResponseEntity.ok(response);
    }
}