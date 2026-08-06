package com.sixro.logistics.hub.domain.model;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.hub.domain.exception.HubErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_hub")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Hub extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hub_id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "hub_name", nullable = false, length = 50)
    private String hubName;

    @Embedded
    private Address address;

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "hub_zone", nullable = false, length = 20)
    private HubZone hubZone;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "hub_status", nullable = false, length = 20)
    private HubStatus hubStatus;

    @Builder
    public Hub(String hubName, Address address, Point location, HubZone hubZone, int maxCapacity) {
        validateCapacity(maxCapacity);

        this.hubName = hubName;
        this.address = address;
        this.location = location;
        this.hubZone = hubZone;
        this.maxCapacity = maxCapacity;
        this.hubStatus = HubStatus.ACTIVE; // 신규 허브는 기본적으로 ACTIVE 상태
    }

    public void update(String hubName, Address address, Point location, HubZone hubZone, int maxCapacity) {
        this.hubName = hubName;
        this.address = address;
        this.location = location;
        this.hubZone = hubZone;
        this.maxCapacity = maxCapacity;
    }

    public void changeStatus(HubStatus newStatus) {
        // 상태 전이 규칙 검증
        if (!this.hubStatus.isTransitionableTo(newStatus)) {
            throw new BaseException(HubErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.hubStatus = newStatus;
    }

    private static final int MIN_CAPACITY = 10_000; // 최소 CAPA는 일일 10,000 박스

    private void validateCapacity(int maxCapacity) {
        if (maxCapacity < MIN_CAPACITY) {
            throw new BaseException(HubErrorCode.INVALID_MAX_CAPACITY);
        }
    }
}