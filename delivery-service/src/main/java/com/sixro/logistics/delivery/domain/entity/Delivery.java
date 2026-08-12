package com.sixro.logistics.delivery.domain.entity;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(schema = "delivery_schema", name = "p_delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deliveryId;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID orderId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            schema = "delivery_schema",
            name = "p_delivery_supplier_company",
            joinColumns = @JoinColumn(name = "delivery_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_delivery_supplier_company",
                    columnNames = {"delivery_id", "supplier_company_id"}
            )
    )
    @Column(name = "supplier_company_id", nullable = false)
    private Set<UUID> supplierCompanyIds = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private UUID recipientCompanyId;

    @Column(nullable = false, updatable = false)
    private UUID originHubId;

    @Column(nullable = false)
    private UUID destHubId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_manager_id")
    private DeliveryManager deliveryManager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.HUB_WAITING;

    @Column(length = 500, nullable = false)
    private String deliveryAddress;

    private LocalDateTime deliveryDeadline;

    private String requests;

    @Column(length = 50, nullable = false)
    private String recipientName;

    @Column(length = 100, nullable = false)
    private String recipientSlackId;

    public void startHubTransit() {
        this.deliveryStatus = DeliveryStatus.HUB_IN_TRANSIT;
    }

    public void arriveDestinationHub() {
        this.deliveryStatus = DeliveryStatus.DESTINATION_HUB_ARRIVED;
    }

    public void deliveryFailed() {
        this.deliveryStatus = DeliveryStatus.FAILED;
    }

    public void assignDeliveryManager(DeliveryManager deliveryManager) {
        this.deliveryManager = deliveryManager;
    }

    public void updateInfo(String deliveryAddress, LocalDateTime deliveryDeadline, String requests, String recipientName, String recipientSlackId) {
        if (this.deliveryStatus != DeliveryStatus.HUB_WAITING) {
            throw new BaseException(DeliveryErrorCode.INVALID_DELIVERY_STATUS_TRANSITION);
        }
        if (deliveryAddress != null) {
            this.deliveryAddress = deliveryAddress;
        }
        if (deliveryDeadline != null) {
            this.deliveryDeadline = deliveryDeadline;
        }
        if (requests != null) {
            this.requests = requests;
        }
        if (recipientName != null) {
            this.recipientName = recipientName;
        }
        if (recipientSlackId != null) {
            this.recipientSlackId = recipientSlackId;
        }
    }

    public void updateStatus(DeliveryStatus deliveryStatus) {
        if (this.deliveryStatus == deliveryStatus) {
            return;
        }
        if (!canTransitTo(deliveryStatus)) {
            throw new BaseException(DeliveryErrorCode.INVALID_DELIVERY_STATUS_TRANSITION);
        }

        this.deliveryStatus = deliveryStatus;
    }

    private boolean canTransitTo(DeliveryStatus deliveryStatus) {
        return switch (this.deliveryStatus) {
            case HUB_WAITING -> deliveryStatus == DeliveryStatus.CANCELLED || deliveryStatus == DeliveryStatus.FAILED;
            case HUB_IN_TRANSIT -> deliveryStatus == DeliveryStatus.FAILED;
            case DESTINATION_HUB_ARRIVED -> deliveryStatus == DeliveryStatus.COMPANY_DELIVERY_IN_PROGRESS || deliveryStatus == DeliveryStatus.FAILED;
            case COMPANY_DELIVERY_IN_PROGRESS -> deliveryStatus == DeliveryStatus.DELIVERED || deliveryStatus == DeliveryStatus.FAILED;
            case DELIVERED, CANCELLED, FAILED -> false;
        };
    }
}
