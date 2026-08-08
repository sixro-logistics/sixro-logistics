package com.sixro.logistics.hub.domain.model;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.domain.exception.HubErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Column(name = "zipcode", length = 10, nullable = false)
    private String zipcode;

    @Column(name = "road_address", length = 255, nullable = false)
    private String roadAddress;

    @Column(name = "jibun_address", length = 255)
    private String jibunAddress;

    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    private Address(String zipcode, String roadAddress, String jibunAddress, String detailAddress) {
        if (zipcode == null || zipcode.isBlank() || roadAddress == null || roadAddress.isBlank()) {
            throw new BaseException(HubErrorCode.INVALID_ADDRESS);
        }
        this.zipcode = zipcode;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.detailAddress = detailAddress;
    }

    public static Address of(String zipcode, String roadAddress, String jibunAddress, String detailAddress) {
        return new Address(zipcode, roadAddress, jibunAddress, detailAddress);
    }
}