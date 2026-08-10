package com.sixro.logistics.delivery.infrastructure.repository;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.DeliverySearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryQueryRepository {

    Page<Delivery> search(DeliverySearchCondition condition, Pageable pageable);
}
