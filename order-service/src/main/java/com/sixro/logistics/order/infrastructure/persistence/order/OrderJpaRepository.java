package com.sixro.logistics.order.infrastructure.persistence.order;

import com.sixro.logistics.order.domain.entity.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndIsDeletedFalse(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findForUpdateByIdAndIsDeletedFalse(UUID orderId);

    @Query("""
    select distinct o
    from Order o
    left join fetch o.items
    where o.id = :orderId
      and o.isDeleted = false
    """)
    Optional<Order> findByIdWithItems(UUID orderId);

}
