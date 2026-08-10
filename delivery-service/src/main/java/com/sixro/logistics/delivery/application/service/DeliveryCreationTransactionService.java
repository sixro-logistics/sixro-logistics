package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.delivery.application.model.DeliveryCreationData;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.port.DeliveryManagerRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeliveryCreationTransactionService {

    private final DeliveryRepositoryPort deliveryRepositoryPort;
    private final DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;
    private final DeliveryManagerRepositoryPort deliveryManagerRepositoryPort;

    public DeliveryCreationTransactionService(DeliveryRepositoryPort deliveryRepositoryPort,
                                              DeliveryRouteRepositoryPort deliveryRouteRepositoryPort,
                                              DeliveryManagerRepositoryPort deliveryManagerRepositoryPort) {
        this.deliveryRepositoryPort = deliveryRepositoryPort;
        this.deliveryRouteRepositoryPort = deliveryRouteRepositoryPort;
        this.deliveryManagerRepositoryPort = deliveryManagerRepositoryPort;
    }

    @Transactional
    public boolean create(DeliveryCreationData data) {
        // 주문 이벤트 중복 처리
        if (deliveryRepositoryPort.existsByOrderIdIncludingDeleted(data.orderId())) {
            return false;
        }

        // 자동 배정 후보 잠금
        List<DeliveryManager> hubManagers = data.routes().isEmpty()
                ? List.of() : deliveryManagerRepositoryPort.findAvailableHubManagersForUpdate();
        List<DeliveryManager> companyManagers =
                deliveryManagerRepositoryPort.findAvailableCompanyManagersForUpdate(data.destHubId());

        // 배송 생성 및 업체 담당자 배정
        Delivery delivery = Delivery.create(
                data.orderId(), data.supplierCompanyIds(), data.recipientCompanyId(),
                data.originHubId(), data.destHubId(), data.deliveryAddress(),
                data.deliveryDeadline(), data.requests(), data.recipientName(), data.recipientSlackId());

        Optional<DeliveryManager> selectedManager = selectNextCompanyManager(companyManagers, data.destHubId());
        if (selectedManager.isPresent()) {
            DeliveryManager deliveryManager = selectedManager.get();
            delivery.assignDeliveryManager(deliveryManager);
        }

        Delivery savedDelivery = deliveryRepositoryPort.save(delivery);

        // 배송경로 생성 및 허브 담당자 배정
        List<DeliveryRoute> deliveryRoutes = createDeliveryRoutes(data, savedDelivery, hubManagers);

        // 배송경로 일괄 저장
        if (!deliveryRoutes.isEmpty()) {
            deliveryRouteRepositoryPort.saveAll(deliveryRoutes);
        }

        return true;
    }

    private Optional<DeliveryManager> selectNextCompanyManager(List<DeliveryManager> managers, UUID hubId) {
        if (managers.isEmpty()) {
            return Optional.empty();
        }

        // 최근 업체 담당자 배정 순번 조회
        Integer lastSequence = deliveryManagerRepositoryPort
                .findLastAssignedCompanyManagerSequence(hubId).orElse(null);

        // 다음 업체 담당자 선택
        int nextIndex = findNextIndex(managers, lastSequence);
        DeliveryManager selectedManager = managers.get(nextIndex);

        return Optional.of(selectedManager);
    }

    private List<DeliveryRoute> createDeliveryRoutes(DeliveryCreationData data, Delivery delivery,
                                                     List<DeliveryManager> managers) {
        // 최근 허브 담당자 배정 순번 조회
        Integer lastSequence = managers.isEmpty() ? null
                : deliveryManagerRepositoryPort.findLastAssignedHubManagerSequence().orElse(null);
        int managerIndex = findNextIndex(managers, lastSequence);

        // 경로별 허브 담당자 순차 배정
        List<DeliveryRoute> deliveryRoutes = new ArrayList<>();

        for (DeliveryCreationData.RouteData routeData : data.routes()) {
            DeliveryRoute deliveryRoute = DeliveryRoute.create(
                    delivery, routeData.routeSequence(), routeData.originHubId(), routeData.destHubId(),
                    routeData.expectedDistanceM(), routeData.expectedDurationS());

            if (!managers.isEmpty()) {
                deliveryRoute.assignDeliveryManager(managers.get(managerIndex));
                managerIndex = (managerIndex + 1) % managers.size();
            }

            deliveryRoutes.add(deliveryRoute);
        }

        return deliveryRoutes;
    }

    private int findNextIndex(List<DeliveryManager> managers, Integer lastSequence) {
        if (managers.isEmpty() || lastSequence == null) {
            return 0;
        }

        // 최근 순번 이후의 첫 활성 담당자 조회
        for (int index = 0; index < managers.size(); index++) {
            if (managers.get(index).getDeliverySequence() > lastSequence) {
                return index;
            }
        }

        // 마지막 순번 이후 첫 담당자로 순환
        return 0;
    }
}
