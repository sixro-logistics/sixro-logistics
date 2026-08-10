package com.sixro.logistics.auth.infrastructure.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.auth.application.service.UserAuthStateInvalidationService;
import com.sixro.logistics.auth.infrastructure.messaging.event.UserSecurityChangedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSecurityEventConsumer {

    private final ObjectMapper objectMapper;
    private final UserAuthStateInvalidationService
            invalidationService;

    @KafkaListener(
            topics = {
                    "user-deactivated",
                    "user-role-changed",
                    "user-affiliation-changed"
            },
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) {
        try {
            UserSecurityChangedMessage message =
                    objectMapper.readValue(
                            payload,
                            UserSecurityChangedMessage.class
                    );

            invalidationService.invalidate(
                    message.userId()
            );

            log.info(
                    "사용자 인증 상태 무효화 완료. userId={}",
                    message.userId()
            );

        } catch (JsonProcessingException exception) {
            /*
             * 예외를 밖으로 전달해야 Kafka 재시도 또는
             * 이후 설정할 DLT 처리가 가능합니다.
             */
            throw new IllegalArgumentException(
                    "사용자 보안 이벤트 역직렬화 실패",
                    exception
            );
        }
    }
}