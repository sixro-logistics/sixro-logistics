package com.sixro.logistics.company.domain.entity;

import com.sixro.logistics.common.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_company")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    /**
     * MSA 구조이므로 Hub Entity와 직접 연관관계를 맺지 않고
     * hubId만 UUID로 관리합니다.
     */
    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "company_name", length = 100, nullable = false)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", length = 20, nullable = false)
    private CompanyType companyType;

    @Column(name = "business_number", length = 20, nullable = false, unique = true)
    private String businessNumber;

    @Column(name = "zipcode", length = 20, nullable = false)
    private String zipcode;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "detail_address", length = 255, nullable = false)
    private String detailAddress;

    @Column(name = "contact_name", length = 50, nullable = false)
    private String contactName;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30, nullable = false)
    private String contactPhone;

    public void update(
            UUID hubId,
            String companyName,
            CompanyType companyType,
            String businessNumber,
            String zipcode,
            String address,
            String detailAddress,
            String contactName,
            String contactEmail,
            String contactPhone
    ) {
        if (hubId != null) this.hubId = hubId;
        if (companyName != null) this.companyName = companyName;
        if (companyType != null) this.companyType = companyType;
        if (businessNumber != null) this.businessNumber = businessNumber;
        if (zipcode != null) this.zipcode = zipcode;
        if (address != null) this.address = address;
        if (detailAddress != null) this.detailAddress = detailAddress;
        if (contactName != null) this.contactName = contactName;
        if (contactEmail != null) this.contactEmail = contactEmail;
        if (contactPhone != null) this.contactPhone = contactPhone;
    }
}
