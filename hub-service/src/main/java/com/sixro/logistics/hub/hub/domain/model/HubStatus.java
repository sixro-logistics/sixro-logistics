package com.sixro.logistics.hub.hub.domain.model;

public enum HubStatus {

    ACTIVE,       // 정상 운영
    CONGESTED,    // 혼잡 (물동량 임계치 초과)
    CLOSED,       // 운영 중단 (허브 폐쇄 또는 장애, 파업, 화재, 기상 악화로 가동 중단)
    MAINTENANCE;  // 점검 중

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
}
