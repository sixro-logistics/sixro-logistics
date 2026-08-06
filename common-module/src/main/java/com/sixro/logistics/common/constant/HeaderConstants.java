package com.sixro.logistics.common.constant;

/**
 * Gateway와 내부 서비스 사이에서 사용하는 HTTP Header 상수입니다.
 *
 * <p>클라이언트가 전달한 내부 인증용 Header는 Gateway에서 제거한 뒤,
 * 검증된 JWT Claim을 기반으로 다시 생성합니다.</p>
 */
public final class HeaderConstants {

    private HeaderConstants() {
        throw new AssertionError("Utility class");
    }

    /**
     * 요청 추적을 위한 고유 식별자입니다.
     *
     * <p>Gateway에서 요청마다 생성하고 각 내부 서비스로 전달합니다.</p>
     */
    public static final String REQUEST_ID = "X-Request-Id";

    /**
     * 사용자 식별자(UUID)입니다.
     */
    public static final String USER_ID = "X-User-Id";

    /**
     * 사용자 이름입니다.
     */
    public static final String USERNAME = "X-User-Name";

    /**
     * 사용자 권한입니다.
     */
    public static final String USER_ROLE = "X-User-Role";

    /**
     * 소속 유형(HUB, COMPANY)입니다.
     */
    public static final String AFFILIATION_TYPE = "X-Affiliation-Type";

    /**
     * 소속 식별자(UUID)입니다.
     */
    public static final String AFFILIATION_ID = "X-Affiliation-Id";
}
