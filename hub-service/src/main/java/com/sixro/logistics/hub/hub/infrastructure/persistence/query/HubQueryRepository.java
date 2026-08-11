package com.sixro.logistics.hub.hub.infrastructure.persistence.query;

import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HubQueryRepository extends Repository<Hub, UUID> {

    Optional<Hub> findById(UUID id);

    @Query("SELECT h FROM Hub h " +
            "WHERE (:hubZone IS NULL OR h.hubZone = :hubZone) " +
            "AND (:hubName IS NULL OR h.hubName LIKE %:hubName%)")
    Page<Hub> searchHubs(@Param("hubZone") HubZone hubZone, @Param("hubName") String hubName, Pageable pageable);

    /**
     * 특정 위/경도 기준 최인접 허브 검색 (PostGIS ST_Distance_Sphere)
     * - CLOSED(폐쇄), MAINTENANCE(점검중) 상태는 배송을 받을 수 없으므로 제외
     * - 가장 가까운 1건만 조회
     * - TODO: 권역 기반 최인접 조회, 경로 기반 최인접 조회 2개로 분리 예정
     */
    @Query(value =
            "SELECT " +
            "h.hub_id AS hubId, " +
            "h.hub_name AS hubName, " +
            "h.hub_status AS hubStatus, " +
            "ST_DistanceSphere(h.location::geometry, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geometry) AS distanceInMeters " +
            "FROM hub_schema.p_hub h " +
            "WHERE h.hub_status NOT IN ('CLOSED', 'MAINTENANCE') " +
            "AND h.is_deleted = false " +
            "ORDER BY distanceInMeters ASC " +
            "LIMIT 1",
            nativeQuery = true
    )
    Optional<NearestHubProjection> findNearestHubWithDistance(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude
    );
}