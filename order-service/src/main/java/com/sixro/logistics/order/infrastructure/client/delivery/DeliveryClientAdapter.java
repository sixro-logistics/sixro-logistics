package com.sixro.logistics.order.infrastructure.client.delivery;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.order.application.model.DeliveryManagerInfo;
import com.sixro.logistics.order.application.port.DeliveryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryClientAdapter implements DeliveryQueryPort {

    private final DeliveryClient deliveryClient;

    @Override
    public DeliveryManagerInfo getDeliveryManagerIds(UUID orderId) {

        CommonResponse<DeliveryClientResponse> response
                = deliveryClient.getDeliveryManagerIds(orderId);

        return new DeliveryManagerInfo(
                response.data().deliveryManagerIds()
        );
    }
}
