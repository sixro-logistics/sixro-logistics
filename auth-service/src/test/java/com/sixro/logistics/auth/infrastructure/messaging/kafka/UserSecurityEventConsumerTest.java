package com.sixro.logistics.auth.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.auth.application.service.UserAuthStateInvalidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 사용자 비활성화, 권한 변경 및 소속 변경 Kafka 이벤트를 소비하여
 * 기존 인증 상태를 무효화하는 동작을 검증하는 단위 테스트입니다.
 *
 * <p>실제 Kafka와 Redis를 사용하지 않고 ObjectMapper와 Mock 서비스를 통해
 * 정상 메시지 전달, 역직렬화 실패 및 무효화 처리 예외 전파를 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserSecurityEventConsumerTest {

    @Mock
    private UserAuthStateInvalidationService invalidationService;

    private UserSecurityEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UserSecurityEventConsumer(
                new ObjectMapper(),
                invalidationService
        );
    }

    @Test
    void 사용자_보안_이벤트를_수신하면_인증_상태를_무효화한다() {
        UUID userId = UUID.randomUUID();
        String payload = """
                {
                  "userId": "%s",
                  "role": "HUB_ADMIN",
                  "ignoredField": "ignored"
                }
                """.formatted(userId);

        consumer.consume(payload);

        verify(invalidationService).invalidate(userId);
    }

    @Test
    void JSON_형식이_잘못되면_예외를_전파한다() {
        String payload = """
            {
              "role": "HUB_ADMIN"
            }
            """;

        assertThatThrownBy(
                () -> consumer.consume(payload)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "사용자 보안 이벤트 역직렬화에 실패했습니다."
                )
                .hasCauseInstanceOf(
                        com.fasterxml.jackson.core
                                .JsonProcessingException.class
                );

        verify(
                invalidationService,
                never()
        ).invalidate(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void userId가_없는_이벤트는_예외를_전파한다() {
        String payload = """
            {
              "role": "HUB_ADMIN"
            }
            """;

        assertThatThrownBy(
                () -> consumer.consume(payload)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "사용자 보안 이벤트 역직렬화에 실패했습니다."
                )
                .hasCauseInstanceOf(
                        com.fasterxml.jackson.core
                                .JsonProcessingException.class
                );

        verify(
                invalidationService,
                never()
        ).invalidate(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void 인증_상태_무효화에_실패하면_Kafka_재처리를_위해_예외를_전파한다() {
        UUID userId = UUID.randomUUID();
        String payload = "{\"userId\":\"%s\"}".formatted(userId);
        RuntimeException redisException = new RuntimeException("Redis connection failed");

        doThrow(redisException)
                .when(invalidationService)
                .invalidate(userId);

        assertThatThrownBy(() -> consumer.consume(payload))
                .isSameAs(redisException);
    }
}
