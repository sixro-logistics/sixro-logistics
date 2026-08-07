package com.sixro.logistics.order.domain.entity;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.order.exception.OrderErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_order_item"/*, schema = "order"*/)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderItem /* extends BaseEntity */ {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id", updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, updatable = false)
    private String productName;

    @Column(name = "product_price", nullable = false, updatable = false)
    private Integer productPrice;

    @Column(nullable = false)
    private Integer quantity;

    // 공급업체는 수정이 불가하여 필요 없으면 나중에 삭제
    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    private OrderItem(
            UUID orderId,
            UUID productId,
            String productName,
            Integer productPrice,
            Integer quantity,
            UUID companyId
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.companyId = companyId;
    }

    public static OrderItem create(
            UUID orderId,
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
                orderId,
                productId,
                productName,
                productPrice,
                quantity,
                companyId
        );
    }

}
