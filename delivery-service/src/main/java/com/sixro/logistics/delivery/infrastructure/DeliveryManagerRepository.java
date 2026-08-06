package com.sixro.logistics.delivery.infrastructure;

import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    long countByManagerTypeAndHubIdIsNull(ManagerType managerType);

    long countByManagerTypeAndHubId(ManagerType managerType, UUID hubId);

    @Query(value = """
            select max(delivery_sequence)
            from p_delivery_manager
            where manager_type = 'HUB_DELIVERY'
              and hub_id is null
            """, nativeQuery = true)
    Optional<Integer> findMaxHubDeliverySequenceIncludingDeleted();

    @Query(value = """
            select max(delivery_sequence)
            from p_delivery_manager
            where manager_type = 'COMPANY_DELIVERY'
              and hub_id = :hubId
            """, nativeQuery = true)
    Optional<Integer> findMaxCompanyDeliverySequenceIncludingDeleted(@Param("hubId") UUID hubId);

}
