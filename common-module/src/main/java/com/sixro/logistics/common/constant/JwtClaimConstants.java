package com.sixro.logistics.common.constant;

public final class JwtClaimConstants {

    private JwtClaimConstants() {
        throw new AssertionError("Utility class");
    }

    public static final String USERNAME = "username";
    public static final String ROLE = "role";
    public static final String AFFILIATION_ID = "affiliationId";
    public static final String AFFILIATION_TYPE = "affiliationType";
    public static final String SESSION_ID = "sessionId";
    public static final String TOKEN_TYPE = "tokenType";
}