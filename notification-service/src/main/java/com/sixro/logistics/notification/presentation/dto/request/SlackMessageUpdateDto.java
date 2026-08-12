package com.sixro.logistics.notification.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class SlackMessageUpdateDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String messageContent;
    }
}
