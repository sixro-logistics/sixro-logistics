package com.sixro.logistics.gateway.application.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Redis에 저장된 사용자의 현재 로그인 Session을 조회하여
 * Access Token이 현재 유효한 로그인 Session에 속하는지 검증합니다.
 *
 * <p>Auth Service는 로그인 시
 * {@code session:{userId}} Key에 현재 {@code sessionId}를 저장합니다.</p>
 *
 * <p>Gateway는 JWT에 포함된 {@code sessionId}와
 * Redis의 현재 Session을 비교하여,
 * 새 로그인으로 교체된 이전 Access Token의 사용을 차단합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class SessionValidationService {

    /**
     * Auth Service와 동일한 Redis Session Key 규칙을 사용합니다.
     *
     * <pre>
     * session:{userId}
     * </pre>
     */
    private static final String SESSION_PREFIX = "session:";

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * JWT의 Session ID가 현재 Redis에 저장된
     * 사용자의 로그인 Session과 일치하는지 확인합니다.
     *
     * <p>현재 Session이 존재하지 않는 경우에는 {@code false}를 반환합니다.</p>
     *
     * <p>Redis 연결 장애 등 조회 과정에서 발생하는 예외는
     * 이 계층에서 처리하지 않고 상위 WebFilter로 전달합니다.
     * Gateway 보안 필터에서 Fail Closed 정책에 따라 요청을 차단합니다.</p>
     *
     * @param userId    JWT subject에서 추출한 사용자 식별자
     * @param sessionId JWT sessionId Claim에서 추출한 로그인 세션 식별자
     * @return 현재 로그인 Session과 일치하면 {@code true}
     */
    public Mono<Boolean> isCurrentSession(
            UUID userId,
            UUID sessionId
    ) {
        return redisTemplate.opsForValue()
                .get(createSessionKey(userId))
                .map(savedSessionId ->
                        sessionId.toString()
                                .equals(savedSessionId)
                )
                /*
                 * 로그아웃 등으로 Session Key가 존재하지 않는 경우
                 * 현재 유효한 로그인 Session이 아닌 것으로 처리합니다.
                 */
                .defaultIfEmpty(false);
    }

    /**
     * Auth Service와 동일한 사용자 Session Key를 생성합니다.
     */
    private String createSessionKey(UUID userId) {
        return SESSION_PREFIX + userId;
    }
}