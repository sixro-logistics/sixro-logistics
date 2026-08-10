package com.sixro.logistics.delivery.infrastructure.repository;

import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryManager> findByDeliveryManagerId(UUID deliveryManagerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select dm
            from DeliveryManager dm
            where dm.managerType = com.sixro.logistics.delivery.domain.enums.ManagerType.HUB_DELIVERY
              and dm.hubId is null
              and dm.managerStatus = com.sixro.logistics.delivery.domain.enums.ManagerStatus.AVAILABLE
            order by dm.deliverySequence asc
            """)
    List<DeliveryManager> findAvailableHubManagersForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select dm
            from DeliveryManager dm
            where dm.managerType = com.sixro.logistics.delivery.domain.enums.ManagerType.COMPANY_DELIVERY
              and dm.hubId = :hubId
              and dm.managerStatus = com.sixro.logistics.delivery.domain.enums.ManagerStatus.AVAILABLE
            order by dm.deliverySequence asc
            """)
    List<DeliveryManager> findAvailableCompanyManagersForUpdate(UUID hubId);

    @Query(value = """
            select dm.delivery_sequence
            from delivery_schema.p_delivery_route dr
            join delivery_schema.p_delivery d
              on d.delivery_id = dr.delivery_id
            join delivery_schema.p_delivery_manager dm
              on dm.delivery_manager_id = dr.delivery_manager_id
            where dr.delivery_manager_id is not null
            order by d.created_at desc, dr.route_sequence desc
            limit 1
            """, nativeQuery = true)
    Optional<Integer> findLastAssignedHubManagerSequence();

    @Query(value = """
            select dm.delivery_sequence
            from delivery_schema.p_delivery d
            join delivery_schema.p_delivery_manager dm
              on dm.delivery_manager_id = d.delivery_manager_id
            where d.dest_hub_id = :hubId
              and d.delivery_manager_id is not null
            order by d.created_at desc, d.delivery_id desc
            limit 1
            """, nativeQuery = true)
    Optional<Integer> findLastAssignedCompanyManagerSequence(UUID hubId);

    long countByManagerTypeAndHubIdIsNull(ManagerType managerType);

    long countByManagerTypeAndHubId(ManagerType managerType, UUID hubId);

    @Query(value = """
            select max(delivery_sequence)
            from delivery_schema.p_delivery_manager
            where manager_type = 'HUB_DELIVERY'
              and hub_id is null
            """, nativeQuery = true)
    Optional<Integer> findMaxHubDeliverySequenceIncludingDeleted();

    @Query(value = """
            select max(delivery_sequence)
            from delivery_schema.p_delivery_manager
            where manager_type = 'COMPANY_DELIVERY'
              and hub_id = :hubId
            """, nativeQuery = true)
    Optional<Integer> findMaxCompanyDeliverySequenceIncludingDeleted(@Param("hubId") UUID hubId);


    @Query("""
          SELECT dm
          FROM DeliveryManager dm
          WHERE (:managerType IS NULL
                 OR dm.managerType = :managerType)
            AND (:hubId IS NULL
                 OR dm.hubId = :hubId)
            AND (:managerStatus IS NULL
                 OR dm.managerStatus = :managerStatus)
            AND (:deliverySequence IS NULL
                 OR dm.deliverySequence = :deliverySequence)
          """)
    Page<DeliveryManager> searchDeliveryManagers(ManagerType managerType, UUID hubId, ManagerStatus managerStatus, Integer deliverySequence, Pageable pageable);
}
