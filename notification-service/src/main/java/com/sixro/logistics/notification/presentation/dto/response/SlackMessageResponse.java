package com.sixro.logistics.notification.presentation.dto.response;

import com.sixro.logistics.notification.domain.entity.SendStatus;
import com.sixro.logistics.notification.domain.entity.SenderType;
import com.sixro.logistics.notification.domain.entity.SlackMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SlackMessageResponse {
    private UUID slackMessageId;
    private SenderType senderType;
    private UUID senderId;
    private String senderSlackId;
    private String receiverSlackId;
    private String messageContent;
    private String messageType;
    private SendStatus sendStatus;
    private String errorMessage;

    public static SlackMessageResponse from(SlackMessage entity) {
        return SlackMessageResponse.builder()
                .slackMessageId(entity.getSlackMessageId())
                .senderType(entity.getSenderType())
                .senderId(entity.getSenderId())
                .senderSlackId(entity.getSenderSlackId())
                .receiverSlackId(entity.getReceiverSlackId())
                .messageContent(entity.getMessageContent())
                .messageType(entity.getMessageType())
                .sendStatus(entity.getSendStatus())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}
