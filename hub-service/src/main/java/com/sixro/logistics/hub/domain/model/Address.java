package com.sixro.logistics.hub.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Address {

    @Column(name = "zipcode", length = 10, nullable = false)
    private String zipcode;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    public static Address of(String zipcode, String address, String detailAddress) {
        return new Address(zipcode, address, detailAddress);
    }
}
