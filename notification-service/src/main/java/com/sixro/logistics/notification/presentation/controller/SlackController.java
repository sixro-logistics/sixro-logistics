package com.sixro.logistics.notification.presentation.controller;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.notification.application.client.SlackApiClient;
import com.sixro.logistics.notification.application.service.AiService;
import com.sixro.logistics.notification.application.service.SlackService;
import com.sixro.logistics.notification.domain.event.DeliveryCreatedEvent;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSearchCondition;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSendRequest;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageUpdateDto;
import com.sixro.logistics.notification.presentation.dto.response.SlackMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Tag(name = "Notification", description = "메시지 목록 조회, 상세 조회, 등록 및 발송, 수정, 삭제 API")
@RestController
@RequestMapping("/api/v1/slack/messages")
@RequiredArgsConstructor
public class SlackController {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AiService aiService;
    private final SlackApiClient slackApiClient;

    private final SlackService slackService;

    @Operation(
            summary = "Slack 메시지 목록 조회(마스터 권한)",
            description = "모든 Slack 메시지 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<CommonResponse<Page<SlackMessageResponse>>> getMessages(
            @RequestHeader("X-User-Role") String userRole,
            SlackMessageSearchCondition condition,
            Pageable pageable)
    {
        // 권한 검증: MASTER 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        Page<SlackMessageResponse> response = slackService.getMessages(condition, pageable);
        return ResponseEntity.ok(CommonResponse.success("메세지 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "Slack 메시지 상세 조회(마스터 권한)",
            description = "(단건) Slack 메시지 상세 조회합니다."
    )
    @GetMapping("/{slackMessageId}")
    public ResponseEntity<CommonResponse<SlackMessageResponse>> getMessage(
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable UUID slackMessageId)
    {
        // 권한 검증: MASTER 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        SlackMessageResponse response = slackService.getMessage(slackMessageId);
        return ResponseEntity.ok(CommonResponse.success("메세지 상세 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "Slack 메시지 등록 및 발송",
            description = "Slack 메시지를 등록 및 발송합니다."
    )
    // Slack 메시지 등록 및 발송 (ALL)
    @PostMapping
    public ResponseEntity<CommonResponse<SlackMessageResponse>> sendMessage(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody SlackMessageSendRequest request) {

        SlackMessageResponse response = slackService.sendMessage(request, userId);
        return ResponseEntity.ok(CommonResponse.success("메세지를 발송하였습니다.", response));
    }


    @Operation(
            summary = "Slack 메시지 수정(마스터 권한)",
            description = "Slack 메시지를 수정합니다."
    )
    @PatchMapping("/{slackMessageId}")
    public ResponseEntity<CommonResponse<SlackMessageResponse>> updateMessage(
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable UUID slackMessageId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody SlackMessageUpdateDto.Request request)
    {
        // 권한 검증: MASTER 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        SlackMessageResponse response = slackService.updateMessage(slackMessageId, request, userId);
        return ResponseEntity.ok(CommonResponse.success("메세지를 수정하였습니다.", response));
    }

    @Operation(
            summary = "Slack 메시지 삭제(마스터 권한)",
            description = "Slack 메시지를 삭제합니다."
    )
    @DeleteMapping("/{slackMessageId}")
    public ResponseEntity<CommonResponse<Void>> deleteMessage(
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable UUID slackMessageId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        // 권한 검증: MASTER 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        slackService.deleteMessage(slackMessageId, userId);

        return ResponseEntity.ok(CommonResponse.success("메세지를 삭제하였습니다."));
    }


    // 1. 임시용 - Kafka를 통한 전체 이벤트 파이프라인 테스트
    @PostMapping("/kafka-delivery-event")
    public ResponseEntity<String> sendTestEvent(@RequestBody DeliveryCreatedEvent event) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                "delivery.events",
                event.getData().getDeliveryId().toString(),
                event
        );

        record.headers().add("event-type", "DeliveryCreatedEvent".getBytes(StandardCharsets.UTF_8));
        record.headers().add("trace-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);
        return ResponseEntity.ok("Kafka 이벤트 발행 완료 (eventId: " + event.getEventId() + ")");
    }
}
