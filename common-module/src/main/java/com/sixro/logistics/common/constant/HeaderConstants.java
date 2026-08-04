package com.sixro.logistics.common.constant;

/**
 * Gateway에서 내부 서비스로 전달하는 HTTP Header 상수입니다.
 */
public final class HeaderConstants {

    private HeaderConstants() {
        throw new AssertionError("Utility class");
    }

    /**
     * 사용자 식별자(UUID)
     */
    public static final String USER_ID = "X-User-Id";

    /**
     * 사용자 이름
     */
    public static final String USERNAME = "X-User-Name";

    /**
     * 사용자 권한
     */
    public static final String USER_ROLE = "X-User-Role";

    /**
     * 소속 유형(HUB, COMPANY)
     */
    public static final String AFFILIATION_TYPE = "X-Affiliation-Type";

    /**
     * 소속 ID(UUID)
     */
    public static final String AFFILIATION_ID = "X-Affiliation-Id";
}
