package com.sixro.logistics.product.domain.entity;

import com.sixro.logistics.common.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE p_product SET is_deleted = true, deleted_at = NOW() WHERE product_id = ?")
@Where(clause = "is_deleted = false")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;


    @Builder
    public Product(UUID companyId, UUID hubId, String productName, String description, BigDecimal price) {
        this.companyId = companyId;
        this.hubId = hubId;
        this.productName = productName;
        this.description = description;
        this.price = price;
    }

    // 비즈니스 메서드: 상품 정보 수정
    public void updateProduct(String productName, String description, BigDecimal price) {
        if (productName != null) this.productName = productName;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
    }
}
