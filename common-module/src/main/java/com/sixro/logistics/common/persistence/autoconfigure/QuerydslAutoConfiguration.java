package com.sixro.logistics.common.persistence.autoconfigure;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * QueryDSL의 {@link JPAQueryFactory}를 자동으로 등록합니다.
 *
 * <p>QueryDSL JPA가 클래스패스에 존재하고,
 * EntityManagerFactory가 구성된 JPA 서비스에만 적용됩니다.</p>
 *
 * <p>JPA를 사용하지 않는 Gateway Service에는 적용되지 않습니다.</p>
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnClass({JPAQueryFactory.class, EntityManager.class})
@ConditionalOnSingleCandidate(EntityManagerFactory.class)
public class QuerydslAutoConfiguration {

    /**
     * QueryDSL 쿼리 작성에 사용하는 JPAQueryFactory를 등록합니다.
     *
     * <p>각 서비스가 자체 JPAQueryFactory Bean을 등록한 경우에는
     * 서비스 설정을 우선하며 공통 Bean은 생성하지 않습니다.</p>
     */
    @Bean
    @ConditionalOnMissingBean(JPAQueryFactory.class)
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
