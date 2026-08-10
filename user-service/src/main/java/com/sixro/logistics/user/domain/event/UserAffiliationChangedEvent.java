package com.sixro.logistics.user.domain.event;

import com.sixro.logistics.user.domain.model.AffiliationType;

import java.util.UUID;

/**
 * 사용자 소속 변경 완료 시 발행하는 도메인 이벤트입니다.
 */
public record UserAffiliationChangedEvent(
        UUID userId,
        UUID previousAffiliationId,
        AffiliationType previousAffiliationType,
        UUID newAffiliationId,
        AffiliationType newAffiliationType
) {
}