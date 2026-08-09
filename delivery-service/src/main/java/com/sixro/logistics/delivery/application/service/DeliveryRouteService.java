package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.util.PageUtil;
import com.sixro.logistics.delivery.application.command.GetDeliveryRouteCommand;
import com.sixro.logistics.delivery.application.command.SearchDeliveryRoutesCommand;
import com.sixro.logistics.delivery.application.result.DeliveryRouteResult;
import com.sixro.logistics.delivery.domain.DeliveryRouteSearchCondition;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.DeliverySearchScope;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
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
public class DeliveryRouteService {

    private final DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;

    public DeliveryRouteService(DeliveryRouteRepositoryPort deliveryRouteRepositoryPort) {
        this.deliveryRouteRepositoryPort = deliveryRouteRepositoryPort;
    }

    @Transactional(readOnly = true)
    public DeliveryRouteResult getDeliveryRoute(GetDeliveryRouteCommand command) {
        DeliveryRoute deliveryRoute = deliveryRouteRepositoryPort.findById(command.getDeliveryRouteId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_NOT_FOUND));

        validateGetAuthority(command, deliveryRoute);

        return new DeliveryRouteResult(deliveryRoute);
    }

    private void validateGetAuthority(GetDeliveryRouteCommand command, DeliveryRoute deliveryRoute) {
        String userRole = command.getUserRole();

        if ("MASTER_ADMIN".equals(userRole)) {
            return;
        }
        if ("HUB_ADMIN".equals(userRole)) {
            validateHubAdminAuthority(command.getAffiliationId(), deliveryRoute);
            return;
        }
        if ("DELIVERY_MANAGER".equals(userRole)) {
            validateDeliveryManagerAuthority(command.getLoginUserId(), deliveryRoute);
            return;
        }
        if ("COMPANY_MANAGER".equals(userRole)) {
            validateCompanyManagerAuthority(command.getAffiliationId(), deliveryRoute.getDelivery());
            return;
        }

        throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
    }

    private void validateHubAdminAuthority(UUID affiliationId, DeliveryRoute deliveryRoute) { // HUB_ADMIN의 소속 허브가 경로의 출발/도착 허브이면 허용
        boolean isRelatedHub = affiliationId != null
                && (Objects.equals(affiliationId, deliveryRoute.getOriginHubId()) || Objects.equals(affiliationId, deliveryRoute.getDestHubId()));

        if (!isRelatedHub) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
        }
    }

    private void validateDeliveryManagerAuthority(UUID loginUserId, DeliveryRoute deliveryRoute) { // 현재 배송경로에 본인(허브배송담당자)이 배정된 경우
        boolean isAssignedDeliveryManager = loginUserId != null && deliveryRoute.getDeliveryManager() != null
                && Objects.equals(loginUserId, deliveryRoute.getDeliveryManager().getDeliveryManagerId());

        if (!isAssignedDeliveryManager) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
        }
    }

    private void validateCompanyManagerAuthority(UUID affiliationId, Delivery delivery) { // 경로가 속한 배송의 공급/수령업체가 본인의 소속업체임
        boolean isRelatedCompany = affiliationId != null
                && (Objects.equals(affiliationId, delivery.getSupplierCompanyId()) || Objects.equals(affiliationId, delivery.getRecipientCompanyId()));

        if (!isRelatedCompany) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
        }
    }

    @Transactional(readOnly = true)
    public Page<DeliveryRouteResult> searchDeliveryRoutes(SearchDeliveryRoutesCommand command) {
        validateSearchAuthority(command);

        Pageable vdPageable = validateSearchPageable(command);

        // 역할별 조회 범위 계산
        DeliverySearchScope searchScope = resolveSearchScope(command.getUserRole());
        UUID scopeId = resolveScopeId(command);

        // 최종 검색 조건 생성
        DeliveryRouteSearchCondition condition = new DeliveryRouteSearchCondition(
                command.getDeliveryId(), command.getRouteStatus(), command.getOriginHubId(), command.getDestHubId(),
                command.getDeliveryManagerId(), command.getRouteSequence(), searchScope, scopeId);

        // 배송 경로 목록 조회 및 결과 변환
        return deliveryRouteRepositoryPort.searchDeliveryRoutes(condition, vdPageable).map(DeliveryRouteResult::new);
    }

    private void validateSearchAuthority(SearchDeliveryRoutesCommand command) {
        String userRole = command.getUserRole();

        if ("MASTER_ADMIN".equals(userRole)) {
            return;
        }
        if ("HUB_ADMIN".equals(userRole) || "COMPANY_MANAGER".equals(userRole)) {
            if (command.getAffiliationId() == null) {
                throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
            }
            return;
        }
        if ("DELIVERY_MANAGER".equals(userRole)) {
            if (command.getLoginUserId() == null) {
                throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
            }
            return;
        }

        throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
    }

    private Pageable validateSearchPageable(SearchDeliveryRoutesCommand command) {
        Pageable pageable = command.getPageable();
        List<Sort.Order> sortOrders = pageable.getSort().stream().toList();

        if (sortOrders.size() > 1) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        if (sortOrders.isEmpty()) {
            String defaultSortField = command.getDeliveryId() == null ? "createdAt" : "sequence";
            String defaultDirection = command.getDeliveryId() == null ? "DESC" : "ASC";
            return PageUtil.toPageable(pageable.getPageNumber(), pageable.getPageSize(), defaultDirection, defaultSortField);
        }

        Sort.Order sortOrder = sortOrders.get(0);
        Set<String> allowedSortFields = Set.of("createdAt", "updatedAt", "sequence", "startedAt", "completedAt");
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
            default -> throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
        };
    }

    private UUID resolveScopeId(SearchDeliveryRoutesCommand command) {
        return switch (command.getUserRole()) {
            case "MASTER_ADMIN" -> null;
            case "DELIVERY_MANAGER" -> command.getLoginUserId();
            case "HUB_ADMIN", "COMPANY_MANAGER" -> command.getAffiliationId();
            default -> throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_FORBIDDEN);
        };
    }
}
