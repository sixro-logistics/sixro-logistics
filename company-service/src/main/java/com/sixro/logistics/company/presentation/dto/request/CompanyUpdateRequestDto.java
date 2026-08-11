package com.sixro.logistics.company.presentation.dto.request;

import com.sixro.logistics.company.domain.entity.CompanyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CompanyUpdateRequestDto {

    private UUID hubId;

    @Size(max = 100)
    private String companyName;

    private CompanyType companyType;

    @Size(max = 20)
    private String businessNumber;

    @Size(max = 20)
    private String zipcode;

    @Size(max = 255)
    private String address;

    @Size(max = 255)
    private String detailAddress;

    @Size(max = 50)
    private String contactName;

    @Email
    @Size(max = 100)
    private String contactEmail;

    @Size(max = 30)
    private String contactPhone;
}
