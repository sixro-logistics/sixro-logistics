package com.sixro.logistics.delivery.presentation.dto.req;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DeliveryInfoUpdateReqDto {

    @Size(max = 500)
    @Pattern(regexp = "(?s).*\\S.*", message = "배송지는 공백만 입력할 수 없습니다.")
    private String deliveryAddress;

    @Future
    private LocalDateTime deliveryDeadline;

    @Size(max = 255)
    private String requests;

    @Size(max = 100)
    @Pattern(regexp = "(?s).*\\S.*", message = "수령인 이름은 공백만 입력할 수 없습니다.")
    private String recipientName;

    @Size(max = 100)
    @Pattern(regexp = "(?s).*\\S.*", message = "수령인 Slack ID는 공백만 입력할 수 없습니다.")
    private String recipientSlackId;
}
