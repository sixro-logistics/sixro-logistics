package com.sixro.logistics.delivery.infrastructure;

import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {
}
