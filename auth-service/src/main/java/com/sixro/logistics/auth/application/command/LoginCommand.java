package com.sixro.logistics.auth.application.command;

/**
 * 로그인 유스케이스에 필요한 입력값을 전달하는 Command입니다.
 *
 * @param username 로그인 사용자명
 * @param password 암호화되지 않은 사용자의 입력 비밀번호
 */
public record LoginCommand(
        String username,
        String password
) {
}