package com.sixro.logistics.order.domain.entity.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.exception.ErrorCode;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.order.exception.OrderErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_order", schema = "order_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false)
    private UUID id;

    @Column(name = "receiver_id", nullable = false, updatable = false)
    private UUID receiverId;

    @Column(name = "hub_id", nullable = false, updatable = false)
    private UUID hubId;

    @Column(name = "receiver_company_id", nullable = false, updatable = false)
    private UUID receiverCompanyId;

    @Column(name = "delivery_address", nullable = false, updatable = false)
    private String deliveryAddress;

    @Column(name = "delivery_deadline", nullable = false)
    private LocalDateTime deliveryDeadline;

    private String requests;

    @OneToMany(
            mappedBy = "order",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL
    )
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    private Order(UUID hubId, UUID receiverId, UUID receiverCompanyId,
                  String deliveryAddress, LocalDateTime deliveryDeadline,
                  String requests){
        this.hubId = hubId;
        this.receiverId = receiverId;
        this.receiverCompanyId = receiverCompanyId;
        this.deliveryAddress = deliveryAddress;
        this.deliveryDeadline = deliveryDeadline;
        this.requests = requests;
        this.orderStatus = OrderStatus.CREATED;
    }

    public static Order create(UUID hubId, UUID receiverId, UUID receiverCompanyId,
                               String deliveryAddress, LocalDateTime deliveryDeadline,
                               String requests){

        if(deliveryDeadline != null && !deliveryDeadline.isAfter(LocalDateTime.now())){
            throw new BaseException(OrderErrorCode.INVALID_DELIVERY_DEADLINE);
        }

        if(requests != null && requests.length() > 255){
            throw new BaseException(OrderErrorCode.INVALID_REQUESTS);
        }

        return new Order(
                hubId,
                receiverId,
                receiverCompanyId,
                deliveryAddress,
                deliveryDeadline,
                requests
        );
    }

    public void addOrderItem(OrderItem orderItem) {
        items.add(orderItem);
        orderItem.assignOrder(this);
    }

    public void fail(){
        this.orderStatus = OrderStatus.FAILED;
    }

    public void update(LocalDateTime deliveryDeadline, String requests) {
        if(deliveryDeadline != null && !deliveryDeadline.isAfter(LocalDateTime.now())){
            throw new BaseException(OrderErrorCode.INVALID_DELIVERY_DEADLINE);
        }

        if(requests != null && requests.length() > 255){
            throw new BaseException(OrderErrorCode.INVALID_REQUESTS);
        }

        if(this.orderStatus != OrderStatus.CREATED){
            throw new BaseException(OrderErrorCode.ORDER_CANNOT_BE_MODIFIED);
        }

        if(deliveryDeadline != null){
            this.deliveryDeadline = deliveryDeadline;
        }

        if(requests != null){
            this.requests = requests;
        }
    }

    public void cancel(){
        if(this.orderStatus != OrderStatus.CREATED){
            throw new BaseException(OrderErrorCode.ORDER_CANNOT_BE_MODIFIED);
        }
        this.orderStatus = OrderStatus.CANCELED;
    }

    public void deliveryCreated(){
        if(this.orderStatus != OrderStatus.CREATED){
            throw new BaseException(OrderErrorCode.ORDER_CANNOT_BE_DELIVERY_CREATED);
        }
        this.orderStatus = OrderStatus.DELIVERY_CREATED;
    }

}
