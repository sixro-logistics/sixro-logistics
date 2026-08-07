package com.sixro.logistics.auth.infrastructure.jwt;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT 서명 검증에 사용할 RSA 공개키를 설정 파일의 리소스에서 읽어옵니다.
 */
@Component
public class RsaPublicKeyLoader {

    private static final String BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String END = "-----END PUBLIC KEY-----";

    private final PublicKey publicKey;

    public RsaPublicKeyLoader(JwtProperties properties) {
        this.publicKey = load(properties.publicKeyPath());
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PublicKey load(String path) {
        try {
            String pem = Files.readString(
                    Path.of(path),
                    StandardCharsets.UTF_8
            );

            if (!pem.contains(BEGIN) || !pem.contains(END)) {
                throw new IllegalStateException(
                        "Public Key는 X.509 PEM 형식이어야 합니다."
                );
            }

            String encoded = pem
                    .replace(BEGIN, "")
                    .replace(END, "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(encoded);

            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "RSA Public Key 파일을 읽을 수 없습니다: " + path,
                    exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "RSA Public Key를 생성할 수 없습니다.",
                    exception
            );
        }
    }
}