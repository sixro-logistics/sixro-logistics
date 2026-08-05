package com.sixro.logistics.delivery.domain.entity;

import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deliveryId;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID orderId;

    @Column(nullable = false)
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
}
