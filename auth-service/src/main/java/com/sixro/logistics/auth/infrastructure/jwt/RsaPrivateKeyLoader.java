package com.sixro.logistics.auth.infrastructure.jwt;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * JWT 서명에 사용할 RSA 비밀키를 설정 파일의 리소스에서 읽어옵니다.
 *
 * <p>비밀키는 외부에 노출하거나 API 응답 및 로그에 출력해서는 안 됩니다.</p>
 * <p>
 * TODO(auth): 운영 환경에서는 classpath 파일 대신
 *  * AWS Secrets Manager, Kubernetes Secret 또는 Vault를 통해 키를 주입한다.
 */
@Component
public class RsaPrivateKeyLoader {

    private static final String BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String END = "-----END PRIVATE KEY-----";

    private final PrivateKey privateKey;

    public RsaPrivateKeyLoader(JwtProperties properties) {
        this.privateKey = load(properties.privateKeyPath());
    }

    // 비밀키를 최초 한 번만 로드하여 재사용합니다.
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    private PrivateKey load(String path) {
        try {
            String pem = Files.readString(
                    Path.of(path),
                    StandardCharsets.UTF_8
            );

            if (!pem.contains(BEGIN) || !pem.contains(END)) {
                throw new IllegalStateException(
                        "Private Key는 PKCS#8 PEM 형식이어야 합니다."
                );
            }

            String encoded = pem
                    .replace(BEGIN, "")
                    .replace(END, "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(encoded);

            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "RSA Private Key 파일을 읽을 수 없습니다: " + path,
                    exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "RSA Private Key를 생성할 수 없습니다.",
                    exception
            );
        }
    }
}