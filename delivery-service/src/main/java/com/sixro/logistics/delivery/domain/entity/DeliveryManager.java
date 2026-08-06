package com.sixro.logistics.delivery.domain.entity;

import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_delivery_manager")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class DeliveryManager extends BaseEntity {

    @Id
    @Column(nullable = false, unique = true)
    private UUID deliveryManagerId;

    private UUID hubId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManagerType managerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManagerStatus managerStatus = ManagerStatus.AVAILABLE;

    @Column(nullable = false)
    private Integer deliverySequence;

    public static DeliveryManager create(UUID userId, UUID hubId, ManagerType managerType, Integer deliverySequence) {
        DeliveryManager deliveryManager = new DeliveryManager();
        deliveryManager.deliveryManagerId = userId;
        deliveryManager.hubId = hubId;
        deliveryManager.managerType = managerType;
        deliveryManager.managerStatus = ManagerStatus.AVAILABLE;
        deliveryManager.deliverySequence = deliverySequence;

        return deliveryManager;
    }

    public void update(UUID hubId, ManagerType managerType, ManagerStatus managerStatus, Integer deliverySequence) {
        this.hubId = hubId;
        this.managerType = managerType;
        this.managerStatus = managerStatus;
        this.deliverySequence = deliverySequence;
    }

}