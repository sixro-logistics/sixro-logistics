package com.sixro.logistics.hub.infrastructure.persistence;

import com.sixro.logistics.hub.domain.model.Hub;
import com.sixro.logistics.hub.domain.model.HubZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HubJpaRepository extends JpaRepository<Hub, UUID> {

    boolean existsByHubName(String hubName);

    @Query("SELECT h FROM Hub h " +
            "WHERE (:hubZone IS NULL OR h.hubZone = :hubZone) " +
            "AND (:hubName IS NULL OR h.hubName LIKE %:hubName%)")
    Page<Hub> searchHubs(@Param("hubZone") HubZone hubZone, @Param("hubName") String hubName, Pageable pageable);

    /**
     * 특정 위/경도 기준 최인접 허브 검색 (PostGIS ST_Distance_Sphere 활용)
     * - CLOSED(폐쇄), MAINTENANCE(점검중) 상태는 배송을 받을 수 없으므로 제외
     * - LIMIT 1로 가장 가까운 1건만 조회 (GiST 인덱스 최적화)
     */
    @Query(value = "SELECT " +
            "h.hub_id AS hubId, " +
            "h.hub_name AS hubName, " +
            "h.hub_status AS hubStatus, " +
            "ST_DistanceSphere(h.location::geometry, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geometry) AS distanceInMeters " +
            "FROM p_hub h " +
            "WHERE h.hub_status NOT IN ('CLOSED', 'MAINTENANCE') " +
            "AND h.is_deleted = false " +
            "ORDER BY distanceInMeters ASC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<NearestHubProjection> findNearestHubWithDistance(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude
    );
}
