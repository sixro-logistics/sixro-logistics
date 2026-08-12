package com.sixro.logistics.auth.infrastructure.client.config;

import feign.FeignException;

import java.util.function.Predicate;

/**
 * Auth Service에서 User Service를 호출한 결과가
 * Circuit Breaker 장애로 기록될지 판단합니다.
 */
public class AuthUserServiceFailurePredicate
        implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {

        if (throwable instanceof FeignException exception) {
            int status = exception.status();

            /*
             * 4xx는 User Service 장애가 아니라
             * 사용자 없음, 비활성 사용자 등 비즈니스 응답입니다.
             *
             * 특히 잘못된 username으로 발생하는 404를 장애로 기록하면
             * 반복된 로그인 실패만으로 Circuit Breaker가 열릴 수 있습니다.
             */
            if (status >= 400 && status < 500) {
                return false;
            }
        }

        /*
         * 연결 실패, Timeout, 5xx 및 Circuit Breaker가 알 수 없는
         * 시스템 예외는 장애로 기록합니다.
         */
        return true;
    }
}