package com.sixro.logistics.notification.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackApiRequest {

    @JsonProperty("channel")
    private String channel; // Slack 채널 ID 또는 사용자 Slack ID

    @JsonProperty("text")
    private String text; // 발송할 메시지 내용
}
