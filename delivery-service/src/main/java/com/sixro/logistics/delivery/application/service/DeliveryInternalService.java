package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.delivery.application.result.DeliveryManagerIdsResult;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DeliveryInternalService {

    private final DeliveryRepositoryPort deliveryRepositoryPort;
    private final DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;

    public DeliveryInternalService(DeliveryRepositoryPort deliveryRepositoryPort, DeliveryRouteRepositoryPort deliveryRouteRepositoryPort) {
        this.deliveryRepositoryPort = deliveryRepositoryPort;
        this.deliveryRouteRepositoryPort = deliveryRouteRepositoryPort;
    }

    public DeliveryManagerIdsResult getDeliveryManagerIds(UUID orderId) {
        // 주문 ID 기준 배송 조회
        Delivery delivery = deliveryRepositoryPort.findByOrderId(orderId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        // 배송경로 담당자 조회
        List<DeliveryRoute> deliveryRoutes = deliveryRouteRepositoryPort
                .findAllByDeliveryId(delivery.getDeliveryId());

        // 미배정 담당자 제외 및 중복 제거
        Set<UUID> deliveryManagerIds = new HashSet<>();
        for (DeliveryRoute deliveryRoute : deliveryRoutes) {
            DeliveryManager deliveryManager = deliveryRoute.getDeliveryManager();

            if (deliveryManager != null) {
                UUID deliveryManagerId = deliveryManager.getDeliveryManagerId();
                deliveryManagerIds.add(deliveryManagerId);
            }
        }

        // 업체 배송 담당자 추가
        DeliveryManager companyDeliveryManager = delivery.getDeliveryManager();
        if (companyDeliveryManager != null) {
            UUID companyDeliveryManagerId = companyDeliveryManager.getDeliveryManagerId();
            deliveryManagerIds.add(companyDeliveryManagerId);
        }

        List<UUID> resultIds = new ArrayList<>(deliveryManagerIds);
        return new DeliveryManagerIdsResult(resultIds);
    }
}
