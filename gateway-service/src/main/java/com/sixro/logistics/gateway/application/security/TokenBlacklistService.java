package com.sixro.logistics.gateway.application.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Redis에 저장된 Access Token 블랙리스트를 조회하는 서비스입니다.
 *
 * <p>로그아웃된 Access Token은 Redis에 TTL과 함께 저장되며,
 * Gateway는 요청마다 해당 토큰의 JTI를 조회하여
 * 이미 로그아웃된 토큰인지 확인합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX =
            "blacklist:access:";

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Access Token의 JTI가 블랙리스트에 등록되어 있는지 확인합니다.
     *
     * <p>Redis 조회 오류는 요청 허용으로 변환하지 않고 호출자에게 전파합니다.
     * Gateway 필터는 해당 오류를 인증 상태 확인 실패로 처리하여
     * Fail Closed 정책에 따라 요청을 차단합니다.</p>
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

    private String createBlacklistKey(String tokenId) {
        return BLACKLIST_PREFIX + tokenId;
    }
}