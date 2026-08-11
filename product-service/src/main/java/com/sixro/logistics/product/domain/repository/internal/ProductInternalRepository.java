package com.sixro.logistics.product.domain.repository.internal;

import com.sixro.logistics.product.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductInternalRepository extends JpaRepository<Product, UUID> {

    // 여러 productId 리스트 중 삭제되지 않은(isDeleted = false) 상품 목록 조회
    List<Product> findAllByProductIdInAndIsDeletedFalse(List<UUID> productIds);

    Optional<Product> findByProductIdAndIsDeletedFalse(UUID productId);
}
