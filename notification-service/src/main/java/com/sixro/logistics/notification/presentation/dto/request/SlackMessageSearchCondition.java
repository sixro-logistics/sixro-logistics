package com.sixro.logistics.notification.presentation.dto.request;

import com.sixro.logistics.notification.domain.entity.SendStatus;
import com.sixro.logistics.notification.domain.entity.SenderType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SlackMessageSearchCondition {
    private SenderType senderType;
    private UUID senderId;
    private String messageType;
    private SendStatus sendStatus;
}