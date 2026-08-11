package com.sixro.logistics.delivery.infrastructure.kafka.event;

public enum DeliveryCreationFailureCode {

    RECEIVER_NOT_FOUND("수령인 정보를 확인할 수 없어 배송 생성에 실패했습니다."),
    DESTINATION_HUB_NOT_FOUND("목적지 허브를 확인할 수 없어 배송 생성에 실패했습니다."),
    HUB_ROUTE_NOT_FOUND("허브 이동 경로를 확인할 수 없어 배송 생성에 실패했습니다."),
    DELIVERY_SAVE_FAILED("배송 정보를 저장하지 못했습니다."),
    DELIVERY_CREATION_FAILED("배송 생성 중 알 수 없는 오류가 발생했습니다.");

    private final String message;

    DeliveryCreationFailureCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
