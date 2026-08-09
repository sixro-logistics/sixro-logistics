package com.sixro.logistics.user.application.event;

import com.sixro.logistics.user.domain.event.*;

/**
 * User Service에서 발생한 사용자 상태 변경 이벤트의
 * 발행을 요청하는 Application Port입니다.
 *
 * <p>Application 계층은 구체적인 메시징 기술에 의존하지 않으며,
 * 실제 이벤트 저장 및 전달 방식은 Infrastructure 계층에서 구현합니다.</p>
 */
public interface UserEventPublisher {

    void publish(UserCreatedEvent event);

    void publish(UserApprovedEvent event);

    void publish(UserRejectedEvent event);

    void publish(UserDeactivatedEvent event);

    void publish(UserRoleChangedEvent event);

    void publish(UserAffiliationChangedEvent event);

}