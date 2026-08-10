package com.sixro.logistics.gateway.application.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Redis에 저장된 Access Token 블랙리스트를 조회하는 서비스입니다.
 *
 * <p>Auth Service에서 로그아웃 처리된 Access Token의 JTI를
 * {@code blacklist:{jti}} 형식으로 저장하며,
 * Gateway는 요청마다 동일한 Key를 조회하여
 * 로그아웃된 토큰의 재사용을 차단합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    /**
     * Auth Service와 동일한 Redis Key 규칙을 사용합니다.
     *
     * <p>Auth:
     * blacklist:{jwtId}</p>
     *
     * <p>Gateway:
     * blacklist:{jwtId}</p>
     */
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Access Token의 JTI가 블랙리스트에 등록되어 있는지 확인합니다.
     *
     * <p>Redis 조회 과정에서 발생하는 예외는 이 계층에서 숨기지 않고
     * 상위 WebFilter로 전달합니다.
     * Gateway 보안 필터는 해당 오류를 Fail Closed 정책에 따라
     * 인증 상태 확인 실패로 처리합니다.</p>
     *
     * @param tokenId Access Token의 JTI
     * @return 블랙리스트에 등록되어 있으면 {@code true}
     */
    public Mono<Boolean> isBlacklisted(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return Mono.just(false);
        }

        return redisTemplate.hasKey(
                createBlacklistKey(tokenId)
        );
    }

    /**
     * Auth Service와 동일한 Access Token blacklist Key를 생성합니다.
     */
    private String createBlacklistKey(String tokenId) {
        return BLACKLIST_PREFIX + tokenId;
    }
}