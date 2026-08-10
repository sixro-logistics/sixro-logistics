package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.delivery.application.model.DeliveryCreationData;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeliveryCreationTransactionService {

    private final DeliveryRepositoryPort deliveryRepositoryPort;
    private final DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;

    public DeliveryCreationTransactionService(DeliveryRepositoryPort deliveryRepositoryPort,
                                              DeliveryRouteRepositoryPort deliveryRouteRepositoryPort) {
        this.deliveryRepositoryPort = deliveryRepositoryPort;
        this.deliveryRouteRepositoryPort = deliveryRouteRepositoryPort;
    }

    @Transactional
    public boolean create(DeliveryCreationData data) {
        // 주문 이벤트 중복 처리
        if (deliveryRepositoryPort.existsByOrderIdIncludingDeleted(data.orderId())) {
            return false;
        }

        // 배송 생성 및 저장
        Delivery delivery = Delivery.create(
                data.orderId(), data.supplierCompanyIds(), data.recipientCompanyId(),
                data.originHubId(), data.destHubId(), data.deliveryAddress(),
                data.deliveryDeadline(), data.requests(), data.recipientName(), data.recipientSlackId());
        Delivery savedDelivery = deliveryRepositoryPort.save(delivery);

        // 배송경로 생성
        List<DeliveryRoute> deliveryRoutes = data.routes().stream()
                .map(route -> DeliveryRoute.create(
                        savedDelivery, route.routeSequence(), route.originHubId(), route.destHubId(),
                        route.expectedDistanceM(), route.expectedDurationS()))
                .toList();

        // 배송경로 일괄 저장
        if (!deliveryRoutes.isEmpty()) {
            deliveryRouteRepositoryPort.saveAll(deliveryRoutes);
        }

        return true;
    }
}
