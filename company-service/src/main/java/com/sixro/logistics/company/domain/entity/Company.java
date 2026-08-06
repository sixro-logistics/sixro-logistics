package com.sixro.logistics.company.domain.entity;

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
public class Company {

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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
            String contactPhone,
            UUID updatedBy
    ) {
        this.hubId = hubId;
        this.companyName = companyName;
        this.companyType = companyType;
        this.businessNumber = businessNumber;
        this.zipcode = zipcode;
        this.address = address;
        this.detailAddress = detailAddress;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.updatedBy = updatedBy;
    }

    public void delete(UUID deletedBy) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}
