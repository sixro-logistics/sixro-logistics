package com.sixro.logistics.auth.domain.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Auth Service에서 발생할 수 있는 인증·인가 관련 오류 코드입니다.
 *
 * <p>각 오류는 클라이언트에 반환할 HTTP 상태, 서비스 오류 코드,
 * 사용자에게 노출 가능한 메시지를 가집니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_USERNAME_OR_PASSWORD(
            HttpStatus.UNAUTHORIZED,
            "A001",
            "아이디 또는 비밀번호가 올바르지 않습니다."
    ),

    INVALID_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "A002",
            "유효하지 않은 Access Token입니다."
    ),

    EXPIRED_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "A003",
            "만료된 Access Token입니다."
    ),

    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "A004",
            "유효하지 않은 Refresh Token입니다."
    ),

    EXPIRED_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "A005",
            "만료된 Refresh Token입니다."
    ),

    REFRESH_TOKEN_NOT_FOUND(
            HttpStatus.UNAUTHORIZED,
            "A006",
            "저장된 Refresh Token을 찾을 수 없습니다."
    ),

    LOGGED_OUT_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "A007",
            "로그아웃 처리된 Access Token입니다."
    ),

    USER_NOT_APPROVED(
            HttpStatus.FORBIDDEN,
            "A008",
            "승인되지 않은 사용자입니다."
    ),

    USER_REJECTED(
            HttpStatus.FORBIDDEN,
            "A009",
            "가입이 거절된 사용자입니다."
    ),

    DEACTIVATED_USER(
            HttpStatus.GONE,
            "A010",
            "비활성화된 사용자입니다."
    ),

    MASTER_ADMIN_SIGN_UP_NOT_ALLOWED(
            HttpStatus.FORBIDDEN,
            "A011",
            "MASTER_ADMIN은 회원가입할 수 없습니다."
    ),

    INVALID_AFFILIATION(
            HttpStatus.BAD_REQUEST,
            "A012",
            "권한과 소속 정보가 일치하지 않습니다."
    ),

    USER_SERVICE_COMMUNICATION_FAILED(
            HttpStatus.BAD_GATEWAY,
            "A013",
            "User Service 통신 중 오류가 발생했습니다."
    ),
    /*
     * User Service의 세부 오류 응답을 확인할 수 없는 경우
     * 사용하는 회원 중복 오류 fallback입니다.
     */
    DUPLICATE_USER(
            HttpStatus.CONFLICT,
            "A014",
            "이미 사용 중인 사용자명 또는 Slack ID입니다."
    ),
    INVALID_SIGN_UP_REQUEST(
            HttpStatus.BAD_REQUEST,
            "A015",
            "회원가입 요청 정보가 올바르지 않습니다."
    ),
    TOKEN_OWNER_MISMATCH(
            HttpStatus.UNAUTHORIZED,
            "A016",
            "Access Token과 Refresh Token의 사용자 또는 세션 정보가 일치하지 않습니다."
    ),
    DUPLICATE_USERNAME(
            HttpStatus.CONFLICT,
            "A017",
            "이미 사용 중인 사용자명입니다."
    ),

    DUPLICATE_SLACK_ID(
            HttpStatus.CONFLICT,
            "A018",
            "이미 사용 중인 Slack ID입니다."
    ),

    ADMIN_USER_CREATE_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "A019",
            "사용자 생성 권한이 없습니다."
    )
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}