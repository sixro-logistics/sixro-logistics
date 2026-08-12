package com.sixro.logistics.notification.presentation.dto.request;

import com.sixro.logistics.notification.domain.entity.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackMessageSendRequest {

    @NotNull(message = "발송 주체 타입은 필수입니다.")
    private SenderType senderType; // USER, SYSTEM

    @NotNull(message = "발송 사용자 ID는 필수입니다.")
    private UUID senderId;

    @NotNull(message = "발신 사용자 ID는 필수입니다.")
    private String senderSlackId; // 명세서 컬럼명 반영 (sender_slack_id)

    @NotNull(message = "수신 사용자 ID는 필수입니다.")
    private String receiverSlackId; // 명세서 컬럼명 반영 (receiver_slack_id)

    @NotBlank(message = "발송 메시지 내용은 필수입니다.")
    private String messageContent;

    @NotNull(message = "메시지 종류는 필수입니다.")
    private String messageType;
}
