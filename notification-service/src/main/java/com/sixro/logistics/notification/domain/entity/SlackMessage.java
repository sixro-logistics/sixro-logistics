package com.sixro.logistics.notification.domain.entity;

import com.sixro.logistics.common.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "p_slack_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SlackMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "slack_message_id", nullable = false)
    private UUID slackMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_slack_id", nullable = false)
    private String senderSlackId;

    @Column(name = "receiver_slack_id", nullable = false)
    private String receiverSlackId;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "message_type", nullable = false, length = 30)
    private String messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false)
    private SendStatus sendStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public void updateContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public void updateSendStatus(SendStatus sendStatus, String errorMessage) {
        this.sendStatus = sendStatus;
        this.errorMessage = errorMessage;
    }
}
