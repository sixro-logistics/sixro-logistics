package com.sixro.logistics.notification.application.service;

import com.sixro.logistics.notification.application.client.SlackApiClient;
import com.sixro.logistics.notification.domain.entity.SendStatus;
import com.sixro.logistics.notification.domain.entity.SlackMessage;
import com.sixro.logistics.notification.domain.repository.SlackMessageRepository;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageCreateDto;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSearchCondition;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSendRequest;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageUpdateDto;
import com.sixro.logistics.notification.presentation.dto.response.SlackMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackService {

    private final SlackApiClient slackApiClient;
    private final SlackMessageRepository slackMessageRepository;
    private final AiService aiService;
    private final RestClient restClient = RestClient.create();


    // Slack 메시지 등록 및 발송 (POST /slack/messages)
    /*@Transactional
    public SlackMessageResponse createAndSendMessage(SlackMessageCreateDto.Request request, UUID currentUserId) {
        // 1. AI API 최종 발송 시한 산출 및 메시지 빌드
        String deadlineText = aiService.calculateDeadline(request.getOrderInfo());
        String content = buildOrderMessage(request.getOrderInfo(), deadlineText);

        // 2. 메시지 엔티티 생성 (초기 상태 PENDING)
        SlackMessage slackMessage = SlackMessage.builder()
                .senderType(request.getSenderType())
                .senderId(request.getSenderId())
                .receiverSlackId(request.getReceiverSlackId())
                .messageContent(content)
                .messageType(request.getMessageType())
                .sendStatus(SendStatus.PENDING)
                .build();

        slackMessageRepository.save(slackMessage);

        // 3. Slack Webhook 실제 발송
        try {
            restClient.post()
                    .uri(slackWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", content))
                    .retrieve()
                    .toBodilessEntity();

            slackMessage.updateSendStatus(SendStatus.SENT, null);
        } catch (Exception e) {
            log.error("Slack 메시지 발송 실패: {}", e.getMessage());
            slackMessage.updateSendStatus(SendStatus.FAILED, e.getMessage());
        }

        return SlackMessageResponse.from(slackMessage);
    }*/

    // Slack 메시지 목록 조회 (GET /slack/messages)
    public Page<SlackMessageResponse> getMessages(SlackMessageSearchCondition condition, Pageable pageable) {
        return slackMessageRepository.searchMessages(condition, pageable)
                .map(SlackMessageResponse::from);
    }

    // Slack 메시지 상세 조회 (GET /slack/messages/{slackMessageId})
    public SlackMessageResponse getMessage(UUID slackMessageId) {
        SlackMessage slackMessage = slackMessageRepository.findBySlackMessageIdAndIsDeletedFalse(slackMessageId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메시지입니다: " + slackMessageId));
        return SlackMessageResponse.from(slackMessage);
    }

    // Slack 메시지 발송 (POST /slack/messages/{slackMessageId})
    @Transactional
    public SlackMessageResponse sendMessage(SlackMessageSendRequest request, UUID userId) {
        // 1. Entity 생성 (초기 상태: PENDING)
        SlackMessage slackMessage = SlackMessage.builder().senderType(request.getSenderType())
                .senderId(request.getSenderId())
                .senderSlackId(request.getSenderSlackId())
                .receiverSlackId(request.getReceiverSlackId())
                .messageContent(request.getMessageContent())
                .messageType(request.getMessageType())
                .sendStatus(SendStatus.PENDING)
                .build();

        // 2. DB 저장
        slackMessageRepository.save(slackMessage);

        // 3. Slack API 발송 처리 및 명세서 내 status/error_message 업데이트
        try {
            boolean isSent = slackApiClient.sendMessage(
                    request.getReceiverSlackId(),
                    request.getMessageContent()
            );

            if (isSent) {
                slackMessage.updateSendStatus(SendStatus.SENT, null);
            } else {
                slackMessage.updateSendStatus(SendStatus.FAILED, "Slack API 발송 응답 실패");
            }
        } catch (Exception e) {
            log.error("Slack 메시지 발송 중 예외 발생 - receiverSlackId: {}", request.getReceiverSlackId(), e);
            slackMessage.updateSendStatus(SendStatus.FAILED, e.getMessage());
        }

        return SlackMessageResponse.from(slackMessage);
    }

    // Slack 메시지 수정 (PATCH /slack/messages/{slackMessageId})
    @Transactional
    public SlackMessageResponse updateMessage(UUID slackMessageId, SlackMessageUpdateDto.Request request, UUID currentUserId) {
        SlackMessage slackMessage = slackMessageRepository.findBySlackMessageIdAndIsDeletedFalse(slackMessageId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메시지입니다: " + slackMessageId));

        slackMessage.updateContent(request.getMessageContent());

        return SlackMessageResponse.from(slackMessage);
    }

    // Slack 메시지 삭제 - 논리 삭제 (DELETE /slack/messages/{slackMessageId})
    @Transactional
    public void deleteMessage(UUID slackMessageId, UUID currentUserId) {
        SlackMessage slackMessage = slackMessageRepository.findBySlackMessageIdAndIsDeletedFalse(slackMessageId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메시지입니다: " + slackMessageId));

        slackMessage.softDelete(currentUserId);
    }

    /*private String buildOrderMessage(SlackMessageCreateDto.OrderInfo info, String deadlineText) {
        return String.format("""
                주문 번호 : %d
                주문자 정보 : %s / %s
                주문 시간 : %s
                상품 정보 : %s %d박스
                요청 사항 : %s
                발송지 : %s
                경유지 : %s
                도착지 : %s
                배송담당자 : %s / %s
                위 내용을 기반으로 도출된 최종 발송 시한은 %s 입니다.
                """,
                info.getOrderId(), info.getCustomerName(), info.getCustomerEmail(),
                info.getOrderTime(), info.getProductName(), info.getQuantity(),
                info.getRequirement(), info.getOrigin(), info.getStopovers(),
                info.getDestination(), info.getManagerName(), info.getManagerEmail(),
                deadlineText
        );
    }*/

}
