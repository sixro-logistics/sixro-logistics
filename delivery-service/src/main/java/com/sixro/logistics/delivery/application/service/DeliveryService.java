package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.util.PageUtil;
import com.sixro.logistics.delivery.application.command.GetDeliveryCommand;
import com.sixro.logistics.delivery.application.command.SearchDeliveriesCommand;
import com.sixro.logistics.delivery.application.result.DeliverySearchResult;
import com.sixro.logistics.delivery.application.result.DeliveryResult;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import com.sixro.logistics.delivery.domain.DeliverySearchCondition;
import com.sixro.logistics.delivery.domain.enums.DeliverySearchScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class DeliveryService {

    private final DeliveryRepositoryPort deliveryRepositoryPort;
    private final DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;

    public DeliveryService(DeliveryRepositoryPort deliveryRepositoryPort, DeliveryRouteRepositoryPort deliveryRouteRepositoryPort) {
        this.deliveryRepositoryPort = deliveryRepositoryPort;
        this.deliveryRouteRepositoryPort = deliveryRouteRepositoryPort;
    }

    @Transactional(readOnly = true)
    public DeliveryResult getDelivery(GetDeliveryCommand command) {
        Delivery delivery = deliveryRepositoryPort.findById(command.getDeliveryId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        validateGetAuthority(command, delivery);

        return new DeliveryResult(delivery);
    }

    private void validateGetAuthority(GetDeliveryCommand command, Delivery delivery) {
        String userRole = command.getUserRole();

        if ("MASTER_ADMIN".equals(userRole)) {
            return;
        }
        if ("HUB_ADMIN".equals(userRole)) { // 출발/도착허브의 ADMIN이면 허용
            validateHubAdminAuthority(command.getAffiliationId(), delivery);
            return;
        }
        if ("DELIVERY_MANAGER".equals(userRole)) {
            validateDeliveryManagerAuthority(command.getLoginUserId(), delivery);
            return;
        }
        if ("COMPANY_MANAGER".equals(userRole)) { // 소속 업체와 관련된 주문의 배송이면 허용
            // TODO: OrderCreatedEvent를 소비: 공급업체/수령업체 ID가 포함된 Delivery 생성이 되었음
            validateCompanyManagerAuthority(command.getAffiliationId(), delivery);
            return;
        }

        throw new BaseException(DeliveryErrorCode.DELIVERY_FORBIDDEN);
    }

    private void validateHubAdminAuthority(UUID affiliationId, Delivery delivery) {
        boolean isRelatedHub = Objects.equals(affiliationId, delivery.getOriginHubId()) || Objects.equals(affiliationId, delivery.getDestHubId());

        if (!isRelatedHub) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_FORBIDDEN);
        }
    }

    private void validateDeliveryManagerAuthority(UUID loginUserId, Delivery delivery) {
        // 최종 업체 배송 담당자인지
        boolean isAssignedDeliveryManager = (delivery.getDeliveryManager() != null)
                && Objects.equals(loginUserId, delivery.getDeliveryManager().getDeliveryManagerId());

        // 포함된 배송경로에 업무가 있는 허브 배송 담당자인지
        boolean isAssignedDeliveryRouteManager = loginUserId != null
                && deliveryRouteRepositoryPort.existsAssignedDeliveryManager(delivery.getDeliveryId(), loginUserId);

        if (!isAssignedDeliveryManager && !isAssignedDeliveryRouteManager) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_FORBIDDEN);
        }
    }

    private void validateCompanyManagerAuthority(UUID affiliationId, Delivery delivery) {
        boolean isRelatedCompany = affiliationId != null
                && (Objects.equals(affiliationId, delivery.getSupplierCompanyId()) || Objects.equals(affiliationId, delivery.getRecipientCompanyId()));

        if (!isRelatedCompany) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_FORBIDDEN);
        }
    }

    @Transactional(readOnly = true)
    public Page<DeliverySearchResult> searchDeliveries(SearchDeliveriesCommand command) {
        validateSearchAuthority(command);

        Pageable vdPageable = validateSearchPageable(command.getPageable());

        // 역할별 조회 범위 계산
        DeliverySearchScope searchScope = resolveSearchScope(command.getUserRole());
        UUID scopeId = resolveScopeId(command);

        // 최종 검색 조건
        DeliverySearchCondition condition = new DeliverySearchCondition(
                command.getOrderId(), command.getDeliveryStatus(), command.getOriginHubId(), command.getDestHubId(),
                command.getDeliveryManagerId(), command.getDeadline(), searchScope, scopeId);

        return deliveryRepositoryPort.searchDeliveries(condition, vdPageable).map(DeliverySearchResult::new);
    }

    private void validateSearchAuthority(SearchDeliveriesCommand command) {
        String userRole = command.getUserRole();

        if ("MASTER_ADMIN".equals(userRole)) {
            return;
        }
        if ("HUB_ADMIN".equals(userRole) || "COMPANY_MANAGER".equals(userRole)) {
            if (command.getAffiliationId() == null) {
                throw new BaseException(DeliveryErrorCode.DELIVERY_LIST_FORBIDDEN);
            }
            return;
        }
        if ("DELIVERY_MANAGER".equals(userRole)) {
            boolean isAnotherManagerSearch = command.getDeliveryManagerId() != null
                    && !Objects.equals(command.getLoginUserId(), command.getDeliveryManagerId());

            if (command.getLoginUserId() == null || isAnotherManagerSearch) {
                throw new BaseException(DeliveryErrorCode.DELIVERY_LIST_FORBIDDEN);
            }
            return;
        }

        throw new BaseException(DeliveryErrorCode.DELIVERY_LIST_FORBIDDEN);
    }

    private Pageable validateSearchPageable(Pageable pageable) {
        List<Sort.Order> sortOrders = pageable.getSort().stream().toList();
        if (sortOrders.size() != 1) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        Sort.Order sortOrder = sortOrders.get(0);
        Set<String> allowedSortFields = Set.of("createdAt", "updatedAt", "deliveryDeadline");
        if (!allowedSortFields.contains(sortOrder.getProperty())) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        return PageUtil.toPageable(pageable.getPageNumber(), pageable.getPageSize(),
                sortOrder.getDirection().name(), sortOrder.getProperty());
    }

    private DeliverySearchScope resolveSearchScope(String userRole) {
        return switch (userRole) {
            case "MASTER_ADMIN" -> DeliverySearchScope.ALL;
            case "HUB_ADMIN" -> DeliverySearchScope.RELATED_HUB;
            case "DELIVERY_MANAGER" -> DeliverySearchScope.ASSIGNED_MANAGER;
            case "COMPANY_MANAGER" -> DeliverySearchScope.RELATED_COMPANY;
            default -> throw new BaseException(DeliveryErrorCode.DELIVERY_LIST_FORBIDDEN);
        };
    }

    private UUID resolveScopeId(SearchDeliveriesCommand command) {
        return switch (command.getUserRole()) {
            case "MASTER_ADMIN" -> null;
            case "DELIVERY_MANAGER" -> command.getLoginUserId();
            case "HUB_ADMIN", "COMPANY_MANAGER" -> command.getAffiliationId();
            default -> throw new BaseException(DeliveryErrorCode.DELIVERY_LIST_FORBIDDEN);
        };
    }
}
