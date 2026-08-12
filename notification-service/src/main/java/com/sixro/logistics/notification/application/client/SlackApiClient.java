package com.sixro.logistics.notification.application.client;

import com.sixro.logistics.notification.presentation.dto.request.SlackApiRequest;
import com.sixro.logistics.notification.presentation.dto.response.SlackApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class SlackApiClient {

    private final RestClient restClient;

    public SlackApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${slack.bot-token}") String botToken
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://slack.com/api")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Slack 메시지 발송 API 호출
     * @param receiverSlackId 수신자 Slack ID (또는 채널 ID)
     * @param messageContent 메시지 본문
     * @return 발송 성공 여부
     */
    public boolean sendMessage(String receiverSlackId, String messageContent) {
        try {
            SlackApiRequest requestPayload = SlackApiRequest.builder()
                    .channel(receiverSlackId)
                    .text(messageContent)
                    .build();

            SlackApiResponse response = restClient.post()
                    .uri("/chat.postMessage")
                    .body(requestPayload)
                    .retrieve()
                    .body(SlackApiResponse.class);

            if (response != null && response.isOk()) {
                log.info("Slack 메시지 발송 성공 - channel: {}", receiverSlackId);
                return true;
            } else {
                String errorMsg = (response != null) ? response.getError() : "Response is null";
                log.error("Slack API 응답 에러 - channel: {}, error: {}", receiverSlackId, errorMsg);
                return false;
            }

        } catch (Exception e) {
            log.error("Slack API 호출 도중 예외 발생 - channel: {}", receiverSlackId, e);
            return false;
        }
    }
}
