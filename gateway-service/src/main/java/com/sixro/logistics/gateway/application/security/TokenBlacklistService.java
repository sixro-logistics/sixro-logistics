package com.sixro.logistics.gateway.application.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Redis에 저장된 Access Token 블랙리스트를 조회하는 서비스입니다.
 *
 * <p>로그아웃된 Access Token은 Redis에 TTL과 함께 저장되며,
 * Gateway는 요청마다 해당 Token의 JTI(Token ID)를 조회하여
 * 이미 로그아웃된 토큰인지 확인합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    /**
     * Redis에 저장하는 Access Token 블랙리스트 Key Prefix
     *
     * 예)
     * blacklist:access:{jti}
     */
    private static final String BLACKLIST_PREFIX = "blacklist:access:";

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Access Token(JTI)이 블랙리스트에 등록되어 있는지 확인합니다.
     *
     * @param tokenId Access Token의 JTI(JWT ID)
     * @return true : 로그아웃된 토큰
     *         false : 정상 토큰
     */
    public Mono<Boolean> isBlacklisted(String tokenId) {
        // Token ID가 없으면 블랙리스트 검사 대상이 아닙니다.
        if (tokenId == null || tokenId.isBlank()) {
            return Mono.just(false);
        }

        return redisTemplate.hasKey(BLACKLIST_PREFIX + tokenId);
    }
}
