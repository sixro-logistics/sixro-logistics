package com.sixro.logistics.hub.hub.domain.model;

public enum HubStatus {

    ACTIVE,       // 정상 운영
    CONGESTED,    // 혼잡 상태 (적재율 80% 초과)
    CLOSED,       // 운영 중단 (허브 폐쇄 또는 장애, 파업, 화재, 기상 악화로 가동 중단)
    MAINTENANCE;  // 점검 상태 (임시 점검 또는 적재율 110% 초과)

    public boolean isTransitionableTo(HubStatus nextStatus) {
        // 자기 자신으로의 변경은 통과
        if (this == nextStatus) return true;

        return switch (this) {
            case ACTIVE -> nextStatus == CONGESTED || nextStatus == MAINTENANCE || nextStatus == CLOSED;
            case CONGESTED -> nextStatus == ACTIVE || nextStatus == MAINTENANCE || nextStatus == CLOSED;
            case MAINTENANCE -> nextStatus == ACTIVE || nextStatus == CLOSED;
            case CLOSED -> nextStatus == MAINTENANCE;
            default -> false;
        };
    }

    public HubStatus determineNewStatus(double utilization, HubStatus currentStatus) {
        if (isOverloaded(utilization)) return MAINTENANCE;      // 110% 초과시 MAINTENANCE 로 변경
        if (isCongested(utilization)) return CONGESTED;         // ACTIVE 상태에서 80% 초과시 CONGESTED 로 변경
        if (isRecoveredToActive(utilization)) return ACTIVE;    // 70% 아래로 감소시 ACTIVE 로 변경
        return null;
    }

    private boolean isOverloaded(double utilization) {
        return utilization > 1.10 && this != HubStatus.MAINTENANCE;
    }

    private boolean isCongested(double utilization) {
        return utilization > 0.80 && utilization <= 1.10 && this == HubStatus.ACTIVE;
    }

    private boolean isRecoveredToActive(double utilization) {
        return utilization < 0.70 && (this == HubStatus.MAINTENANCE || this == HubStatus.CONGESTED);
    }
}
