package com.sixro.logistics.order.domain.entity;

import com.sixro.logistics.common.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_order"/*, schema = "order"*/)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Order /* extends BaseEntity */ {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false)
    private UUID id;

    @Column(name = "orderer_id", nullable = false, updatable = false)
    private UUID ordererId;

    @Column(name = "hub_id", nullable = false, updatable = false)
    private UUID hubId;

    @Column(name = "receiver_company_id", nullable = false, updatable = false)
    private UUID receiverCompanyId;

    @Column(name = "delivery_address", nullable = false, updatable = false)
    private String deliveryAddress;

    @Column(name = "delivery_deadline", nullable = false)
    private LocalDateTime deliveryDeadline;

    private String requests;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    private Order(UUID hubId, UUID ordererId, UUID receiverCompanyId,
                  String deliveryAddress, LocalDateTime deliveryDeadline,
                  String requests){
        this.hubId = hubId;
        this.ordererId = ordererId;
        this.receiverCompanyId = receiverCompanyId;
        this.deliveryAddress = deliveryAddress;
        this.deliveryDeadline = deliveryDeadline;
        this.requests = requests;
        this.orderStatus = OrderStatus.CREATED;
    }

    public static Order create(UUID hubId, UUID ordererId, UUID receiverCompanyId,
                               String deliveryAddress, LocalDateTime deliveryDeadline,
                               String requests){
        return new Order(hubId, ordererId, receiverCompanyId,
                deliveryAddress, deliveryDeadline, requests);
    }

}
