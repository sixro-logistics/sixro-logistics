package com.sixro.logistics.delivery.domain.entity;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(schema = "delivery_schema", name = "p_delivery_manager")
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

    public void startDelivery() {
        if (this.managerStatus != ManagerStatus.AVAILABLE) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE);
        }

        this.managerStatus = ManagerStatus.IN_DELIVERY;
    }

    public void finishDelivery() {
        if (this.managerStatus == ManagerStatus.IN_DELIVERY) {
            this.managerStatus = ManagerStatus.AVAILABLE;
        }
    }

    public void validateUpdateConditions(UUID hubId, ManagerType managerType, ManagerStatus managerStatus) {
        // 현재 배송 중인 담당자의 수정 요청은 모두 차단
        if (this.managerStatus.equals(ManagerStatus.IN_DELIVERY)) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_UPDATE_NOT_ALLOWED);
        }

        // IN_DELIVERY 상태를 직접 지정하는 요청 차단
        if (managerStatus == ManagerStatus.IN_DELIVERY)
            throw new BaseException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_STATUS_TRANSITION);

        // 담당자 유형 기준 유형-허브 조합 검증
        if (managerType == ManagerType.HUB_DELIVERY && hubId != null) {
            throw new BaseException(
                    DeliveryErrorCode.DELIVERY_MANAGER_TYPE_HUB_MISMATCH
            );
        }
        if (managerType == ManagerType.COMPANY_DELIVERY && hubId == null) {
            throw new BaseException(
                    DeliveryErrorCode.DELIVERY_MANAGER_TYPE_HUB_MISMATCH
            );
        }

    }

}
