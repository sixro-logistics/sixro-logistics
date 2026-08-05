package com.sixro.logistics.common.persistence.autoconfigure;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * QueryDSL의 {@link JPAQueryFactory}를 자동으로 등록합니다.
 *
 * <p>common-module을 의존하는 서비스 중 다음 조건을 모두 만족하는
 * 애플리케이션에만 자동 설정이 적용됩니다.</p>
 *
 * <ul>
 *     <li>QueryDSL JPA가 Classpath에 존재할 것</li>
 *     <li>JPA의 EntityManagerFactory가 구성되어 있을 것</li>
 *     <li>서비스가 별도의 JPAQueryFactory Bean을 등록하지 않았을 것</li>
 * </ul>
 *
 * <p>따라서 JPA를 사용하지 않는 Gateway Service에는
 * 이 설정이 적용되지 않습니다.</p>
 */
@AutoConfiguration
@ConditionalOnClass(JPAQueryFactory.class)
@ConditionalOnBean(EntityManagerFactory.class)
public class QuerydslAutoConfiguration {

    /**
     * QueryDSL 쿼리 작성에 사용하는 JPAQueryFactory를 등록합니다.
     *
     * <p>각 서비스가 자체 JPAQueryFactory Bean을 등록한 경우에는
     * 서비스의 설정을 우선하며, 공통 Bean은 생성하지 않습니다.</p>
     *
     * @param entityManager 현재 서비스의 EntityManager
     * @return QueryDSL 쿼리 팩토리
     */
    @Bean
    @ConditionalOnMissingBean(JPAQueryFactory.class)
    public JPAQueryFactory jpaQueryFactory(
            EntityManager entityManager
    ) {
        return new JPAQueryFactory(entityManager);
    }
}
