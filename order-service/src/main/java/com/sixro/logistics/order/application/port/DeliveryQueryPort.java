package com.sixro.logistics.order.application.port;

import com.sixro.logistics.order.application.model.DeliveryManagerInfo;

import java.util.UUID;


public interface DeliveryQueryPort {

    DeliveryManagerInfo getDeliveryManagerIds(UUID orderId);

}
