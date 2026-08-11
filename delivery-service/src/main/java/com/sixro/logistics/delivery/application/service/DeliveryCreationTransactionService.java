package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.delivery.application.model.DeliveryCreationData;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.port.DeliveryManagerRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import com.sixro.logistics.delivery.application.event.DeliveryCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DeliveryCreationTransactionService {

    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    private final DeliveryRepositoryPort deliveryRepositoryPort;
    private final DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;
    private final DeliveryManagerRepositoryPort deliveryManagerRepositoryPort;
    private final OutboxService outboxService;

    public DeliveryCreationTransactionService(DeliveryRepositoryPort deliveryRepositoryPort,
                                               DeliveryRouteRepositoryPort deliveryRouteRepositoryPort,
                                               DeliveryManagerRepositoryPort deliveryManagerRepositoryPort,
                                               OutboxService outboxService) {
        this.deliveryRepositoryPort = deliveryRepositoryPort;
        this.deliveryRouteRepositoryPort = deliveryRouteRepositoryPort;
        this.deliveryManagerRepositoryPort = deliveryManagerRepositoryPort;
        this.outboxService = outboxService;
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

        // 배송 생성 이벤트 구성
        DeliveryCreatedEvent event = createDeliveryCreatedEvent(data, savedDelivery, deliveryRoutes);

        // Outbox 이벤트 저장
        outboxService.save(event, data.traceId());

        return true;
    }

    private DeliveryCreatedEvent createDeliveryCreatedEvent(
            DeliveryCreationData data, Delivery delivery, List<DeliveryRoute> deliveryRoutes) {

        // 이벤트 기본 정보 생성
        LocalDateTime occurredAt = LocalDateTime.now();
        UUID eventId = UUID.randomUUID();

        // 이벤트 상품 정보 구성
        List<DeliveryCreatedEvent.Product> products = new ArrayList<>();

        for (DeliveryCreationData.ProductData productData : data.products()) {
            DeliveryCreatedEvent.Product product = new DeliveryCreatedEvent.Product(productData.productId(), productData.quantity());
            products.add(product);
        }

        // 이벤트 배송경로 및 총 예상 시간 구성
        long totalHubRouteExpectedDurationS = 0L;
        List<DeliveryCreatedEvent.Route> routes = new ArrayList<>();

        for (DeliveryRoute deliveryRoute : deliveryRoutes) {
            DeliveryCreatedEvent.Route route = new DeliveryCreatedEvent.Route(
                    deliveryRoute.getRouteSequence(), deliveryRoute.getOriginHubId(),
                    deliveryRoute.getDestHubId(), deliveryRoute.getExpectedDurationS()
            );

            routes.add(route);
            totalHubRouteExpectedDurationS += deliveryRoute.getExpectedDurationS();
        }

        // 배송 담당자 근무시간
        DeliveryCreatedEvent.DeliveryManagerWorkingHours workingHours =
                new DeliveryCreatedEvent.DeliveryManagerWorkingHours(WORK_START_TIME, WORK_END_TIME);

        // 배송 담당자 목록 구성
        List<DeliveryCreatedEvent.DeliveryManagerInfo> deliveryManagers = createDeliveryManagerInfos(delivery, deliveryRoutes);

        // 이벤트 데이터 구성
        DeliveryCreatedEvent.DeliveryCreatedData eventData = new DeliveryCreatedEvent.DeliveryCreatedData(
                delivery.getOrderId(), delivery.getDeliveryId(), delivery.getDeliveryDeadline(),
                delivery.getRequests(), delivery.getDeliveryAddress(), products,
                delivery.getOriginHubId(), delivery.getDestHubId(), totalHubRouteExpectedDurationS,
                routes, workingHours, deliveryManagers);

        // 배송 생성 이벤트 생성
        return new DeliveryCreatedEvent(eventId, occurredAt, eventData);
    }

    private List<DeliveryCreatedEvent.DeliveryManagerInfo> createDeliveryManagerInfos(Delivery delivery, List<DeliveryRoute> deliveryRoutes) {

        // 배송 담당자 중복 확인용
        Set<UUID> managerIds = new HashSet<>();
        List<DeliveryManager> managers = new ArrayList<>();

        // 업체 배송 담당자 추가
        if (delivery.getDeliveryManager() != null) {
            DeliveryManager deliveryManager = delivery.getDeliveryManager();
            UUID deliveryManagerId = deliveryManager.getDeliveryManagerId();

            managerIds.add(deliveryManagerId);
            managers.add(deliveryManager);
        }

        // 허브 배송 담당자 추가
        for (DeliveryRoute deliveryRoute : deliveryRoutes) {
            if (deliveryRoute.getDeliveryManager() != null) {
                DeliveryManager deliveryManager = deliveryRoute.getDeliveryManager();
                UUID deliveryManagerId = deliveryManager.getDeliveryManagerId();

                if (!managerIds.contains(deliveryManagerId)) {
                    managerIds.add(deliveryManagerId);
                    managers.add(deliveryManager);
                }
            }
        }

        // 배송 담당자 정렬
        managers.sort((firstManager, secondManager) ->
                Integer.compare(firstManager.getDeliverySequence(), secondManager.getDeliverySequence())
        );

        // 배송 담당자 이벤트 정보 구성
        List<DeliveryCreatedEvent.DeliveryManagerInfo> managerInfos = new ArrayList<>();

        for (DeliveryManager manager : managers) {
            DeliveryCreatedEvent.DeliveryManagerInfo managerInfo =
                    new DeliveryCreatedEvent.DeliveryManagerInfo(
                            manager.getDeliveryManagerId(), manager.getManagerType().name(),
                            manager.getDeliverySequence());

            managerInfos.add(managerInfo);
        }

        return managerInfos;
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
