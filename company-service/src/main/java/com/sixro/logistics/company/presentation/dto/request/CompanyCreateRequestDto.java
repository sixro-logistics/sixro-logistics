package com.sixro.logistics.company.presentation.dto.request;

import com.sixro.logistics.company.domain.entity.CompanyType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CompanyCreateRequestDto {

    @NotNull
    private UUID hubId;

    @NotBlank
    @Size(max = 100)
    private String companyName;

    @NotNull
    private CompanyType companyType;

    @NotBlank
    @Size(max = 20)
    private String businessNumber;

    @NotBlank
    @Size(max = 20)
    private String zipcode;

    @NotBlank
    @Size(max = 255)
    private String address;

    @NotBlank
    @Size(max = 255)
    private String detailAddress;

    @NotBlank
    @Size(max = 50)
    private String contactName;

    @Email
    @Size(max = 100)
    private String contactEmail;

    @NotBlank
    @Size(max = 30)
    private String contactPhone;
}
