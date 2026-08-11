package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.delivery.application.command.CreateDeliveryCommand;
import com.sixro.logistics.delivery.application.command.CreateDeliveryItemCommand;
import com.sixro.logistics.delivery.application.model.CompanyHubInfo;
import com.sixro.logistics.delivery.application.model.DeliveryCreationData;
import com.sixro.logistics.delivery.application.model.HubRoutePathInfo;
import com.sixro.logistics.delivery.application.model.HubRouteProductInfo;
import com.sixro.logistics.delivery.application.model.UserInfo;
import com.sixro.logistics.delivery.application.port.CompanyQueryPort;
import com.sixro.logistics.delivery.application.port.HubRouteQueryPort;
import com.sixro.logistics.delivery.application.port.UserQueryPort;
import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DeliveryCreationService {

    private final UserQueryPort userQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final HubRouteQueryPort hubRouteQueryPort;
    private final DeliveryCreationTransactionService transactionService;

    public DeliveryCreationService(UserQueryPort userQueryPort, CompanyQueryPort companyQueryPort,
                                   HubRouteQueryPort hubRouteQueryPort,
                                   DeliveryCreationTransactionService transactionService) {
        this.userQueryPort = userQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.hubRouteQueryPort = hubRouteQueryPort;
        this.transactionService = transactionService;
    }

    public boolean createDelivery(CreateDeliveryCommand command) {
        // 주문 확정 정보 검증
        validateCommand(command);

        // 수령인 정보 조회
        UserInfo recipient = findRecipient(command.getReceiverId());

        // 목적지 허브 조회
        CompanyHubInfo companyHubInfo = findDestinationHub(command.getReceiverCompanyId());

        // 허브 이동 경로 조회
        List<DeliveryCreationData.RouteData> routes = findRoutes(command, companyHubInfo.destHubId());

        // 공급 업체 정보 구성
        Set<UUID> supplierCompanyIds = command.getOrderItems().stream()
                .map(CreateDeliveryItemCommand::getCompanyId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        // 이벤트용 상품 정보 구성
        List<DeliveryCreationData.ProductData> products = command.getOrderItems().stream()
                .map(item -> new DeliveryCreationData.ProductData(
                        item.getProductId(), item.getQuantity())
                ).toList();

        DeliveryCreationData creationData = new DeliveryCreationData(
                command.getTraceId(), command.getOrderId(), products, supplierCompanyIds, command.getReceiverCompanyId(),
                command.getOriginHubId(), companyHubInfo.destHubId(), command.getDeliveryAddress(),
                command.getDeliveryDeadline(), command.getRequests(), recipient.username(),
                recipient.slackId(), routes);

        // 배송 및 배송경로 저장
        return saveDelivery(creationData);
    }

    private boolean saveDelivery(DeliveryCreationData creationData) {
        try {
            return transactionService.create(creationData);
        } catch (RuntimeException exception) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.DELIVERY_SAVE_FAILED, exception);
        }
    }

    private void validateCommand(CreateDeliveryCommand command) {
        if (command == null || isBlank(command.getTraceId()) || command.getOrderId() == null || command.getOriginHubId() == null
                || command.getReceiverCompanyId() == null || command.getReceiverId() == null
                || command.getDeliveryDeadline() == null || isBlank(command.getDeliveryAddress())
                || command.getDeliveryAddress().length() > 500 || command.getOrderItems() == null
                || command.getOrderItems().isEmpty()
                || command.getRequests() != null && command.getRequests().length() > 255) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        boolean invalidItem = command.getOrderItems().stream().anyMatch(item -> item == null
                || item.getCompanyId() == null || item.getProductId() == null
                || item.getQuantity() == null || item.getQuantity() <= 0);
        if (invalidItem) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private UserInfo findRecipient(UUID receiverId) {
        Optional<UserInfo> recipientOptional;

        try {
            recipientOptional = userQueryPort.findUser(receiverId);
        } catch (RuntimeException exception) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.RECEIVER_NOT_FOUND, exception);
        }

        if (recipientOptional.isEmpty()) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.RECEIVER_NOT_FOUND);
        }

        UserInfo recipient = recipientOptional.get();
        if (!receiverId.equals(recipient.userId()) || isBlank(recipient.username())
                || recipient.username().length() > 50 || isBlank(recipient.slackId())
                || recipient.slackId().length() > 100) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.RECEIVER_NOT_FOUND);
        }

        return recipient;
    }

    private CompanyHubInfo findDestinationHub(UUID receiverCompanyId) {
        Optional<CompanyHubInfo> companyHubInfoOptional;
        try {
            companyHubInfoOptional = companyQueryPort.findHubInfo(receiverCompanyId);
        } catch (RuntimeException exception) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.DESTINATION_HUB_NOT_FOUND, exception);
        }

        if (companyHubInfoOptional.isEmpty()) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.DESTINATION_HUB_NOT_FOUND);
        }

        CompanyHubInfo companyHubInfo = companyHubInfoOptional.get();
        if (!receiverCompanyId.equals(companyHubInfo.destCompanyId()) || companyHubInfo.destHubId() == null) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.DESTINATION_HUB_NOT_FOUND);
        }
        return companyHubInfo;
    }

    private List<DeliveryCreationData.RouteData> findRoutes(CreateDeliveryCommand command, UUID destHubId) {
        if (command.getOriginHubId().equals(destHubId)) {
            return List.of();
        }

        Optional<HubRoutePathInfo> pathOptional;
        try {
            List<HubRouteProductInfo> products = command.getOrderItems().stream()
                    .map(item -> new HubRouteProductInfo(item.getProductId(), item.getQuantity()))
                    .toList();
            pathOptional = hubRouteQueryPort.findPath(command.getOriginHubId(), destHubId, products);
        } catch (RuntimeException exception) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND, exception);
        }

        if (pathOptional.isEmpty()) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        HubRoutePathInfo path = pathOptional.get();
        validateRoutePath(path, command.getOriginHubId(), destHubId);

        return path.routes().stream()
                .map(route -> new DeliveryCreationData.RouteData(
                        route.routeSequence(), route.originHubId(), route.destHubId(),
                        route.expectedDistanceM(), route.expectedDurationS()))
                .toList();
    }

    private void validateRoutePath(HubRoutePathInfo path, UUID originHubId, UUID destHubId) {
        if (!originHubId.equals(path.originHubId()) || !destHubId.equals(path.destHubId())
                || path.routes() == null || path.routes().isEmpty()) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        UUID expectedOriginHubId = originHubId;
        for (int index = 0; index < path.routes().size(); index++) {
            HubRoutePathInfo.RouteInfo route = path.routes().get(index);
            if (route == null || route.routeSequence() == null || route.routeSequence() != index + 1
                    || !expectedOriginHubId.equals(route.originHubId()) || route.destHubId() == null
                    || route.expectedDistanceM() == null || route.expectedDistanceM() < 0
                    || route.expectedDurationS() == null || route.expectedDurationS() < 0) {
                throw new BaseException(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND);
            }
            expectedOriginHubId = route.destHubId();
        }

        if (!destHubId.equals(expectedOriginHubId)) {
            throw new BaseException(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
