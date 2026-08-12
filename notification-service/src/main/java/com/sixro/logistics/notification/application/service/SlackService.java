package com.sixro.logistics.notification.application.service;

import com.sixro.logistics.notification.application.client.SlackApiClient;
import com.sixro.logistics.notification.domain.entity.SendStatus;
import com.sixro.logistics.notification.domain.entity.SlackMessage;
import com.sixro.logistics.notification.domain.repository.SlackMessageRepository;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSearchCondition;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSendRequest;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageUpdateDto;
import com.sixro.logistics.notification.presentation.dto.response.SlackMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

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

}
