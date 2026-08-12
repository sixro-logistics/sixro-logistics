package com.sixro.logistics.order.domain.entity.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.order.exception.OrderErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_order_item", schema = "order_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, updatable = false)
    private String productName;

    @Column(name = "product_price", nullable = false, updatable = false)
    private Integer productPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    private OrderItem(
            UUID productId,
            String productName,
            Integer productPrice,
            Integer quantity,
            UUID companyId
    ) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.companyId = companyId;
    }

    public static OrderItem create(
            UUID productId,
            String productName,
            Integer productPrice,
            Integer quantity,
            UUID companyId
    ) {
        if (quantity == null || quantity < 1) {
            throw new BaseException(OrderErrorCode.INVALID_QUANTITY);
        }

        return new OrderItem(
                productId,
                productName,
                productPrice,
                quantity,
                companyId
        );
    }

    public void assignOrder(Order order) {
        this.order = order;
    }

}
