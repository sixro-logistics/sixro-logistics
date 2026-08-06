package com.sixro.logistics.auth.application.command;

import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;

import java.util.UUID;

/**
 * 회원가입 유스케이스에 필요한 입력값을 애플리케이션 계층으로 전달합니다.
 *
 * <p>Presentation 계층의 형식 검증을 통과한 요청이며,
 * 권한과 소속 정보의 조합은 Auth Service에서 추가로 검증합니다.</p>
 *
 * @param username        사용자명
 * @param password        암호화되지 않은 사용자의 입력 비밀번호
 * @param slackId         Slack 사용자 식별자
 * @param role            가입 신청 권한
 * @param affiliationId   소속 허브 또는 업체 식별자
 * @param affiliationType 소속 유형
 */
public record SignUpCommand(

        String username,
        String password,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType

) {
}