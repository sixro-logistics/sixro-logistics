package com.sixro.logistics.auth.infrastructure.redis;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Refresh Token 원문을 저장하지 않기 위해 단방향 해시값을 생성합니다.
 *
 * <p>동일한 토큰의 저장값 비교가 가능하도록 SHA-256 해시를 사용합니다.
 * 이 클래스는 비밀번호 암호화 용도가 아닙니다.</p>
 */
@Component
public class TokenHashProvider {

    // 전달받은 토큰을 SHA-256 해시 문자열로 변환합니다.
    public String hash(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashed = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 해시 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}