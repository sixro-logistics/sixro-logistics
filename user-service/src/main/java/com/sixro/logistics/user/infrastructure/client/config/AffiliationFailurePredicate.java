package com.sixro.logistics.user.infrastructure.client.config;

import feign.FeignException;

import java.util.function.Predicate;

/**
 * 소속 검증 호출 결과가 Circuit Breaker 장애로 기록될지 판단합니다.
 */
public class AffiliationFailurePredicate
        implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof FeignException exception) {
            int status = exception.status();

            /*
             * 404와 410은 소속 데이터가 존재하지 않는 정상적인
             * 비즈니스 결과이므로 서비스 장애율에 포함하지 않습니다.
             */
            if (status == 404 || status == 410) {
                return false;
            }
        }

        return true;
    }
}