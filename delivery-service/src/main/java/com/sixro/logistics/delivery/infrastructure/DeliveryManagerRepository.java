package com.sixro.logistics.delivery.infrastructure;

import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    @Query("""
              select dm.deliverySequence
              from DeliveryManager dm
              where dm.managerType = :managerType
                and dm.hubId is null
              """)
    List<Integer> findHubDeliveryUsedSequences(@Param("managerType") ManagerType managerType);

    @Query("""
              select dm.deliverySequence
              from DeliveryManager dm
              where dm.managerType = :managerType
                and dm.hubId = :hubId
              """)
    List<Integer> findCompanyDeliveryUsedSequences(@Param("managerType") ManagerType managerType, @Param("hubId") UUID hubId);

}
