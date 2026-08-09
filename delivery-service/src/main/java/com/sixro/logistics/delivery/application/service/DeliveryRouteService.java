package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.util.PageUtil;
import com.sixro.logistics.delivery.application.command.GetDeliveryRouteCommand;
import com.sixro.logistics.delivery.application.command.SearchDeliveryRoutesCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryRouteStatusCommand;
import com.sixro.logistics.delivery.application.result.DeliveryRouteResult;
import com.sixro.logistics.delivery.application.result.DeliveryRouteStatusResult;
import com.sixro.logistics.delivery.domain.DeliveryRouteSearchCondition;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.DeliverySearchScope;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.domain.port.DeliveryManagerRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class DeliveryRouteService {

    private final DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;
    private final DeliveryManagerRepositoryPort deliveryManagerRepositoryPort;

    public DeliveryRouteService(DeliveryRouteRepositoryPort deliveryRouteRepositoryPort,
                                DeliveryManagerRepositoryPort deliveryManagerRepositoryPort) {
        this.deliveryRouteRepositoryPort = deliveryRouteRepositoryPort;
        this.deliveryManagerRepositoryPort = deliveryManagerRepositoryPort;
    }

    @Transactional(readOnly = true)
    public DeliveryRouteResult getDeliveryRoute(GetDeliveryRouteCommand command) {
        DeliveryRoute deliveryRoute = deliveryRouteRepositoryPort.findById(command.getDeliveryRouteId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_NOT_FOUND));

        validateGetAuthority(command, deliveryRoute);

        return new DeliveryRouteResult(deliveryRoute);
    }

    public DeliveryRouteStatusResult updateDeliveryRouteStatus(UpdateDeliveryRouteStatusCommand command) {
        // 배송 경로 조회
        DeliveryRoute deliveryRoute = deliveryRouteRepositoryPort.findByIdForUpdate(command.getDeliveryRouteId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_NOT_FOUND));
        Delivery delivery = deliveryRoute.getDelivery();

        // 변경 권한 검증
        validateUpdateAuthority(command, deliveryRoute);

        // 배송 및 경로 상태 검증
        validateDeliveryStatus(delivery);
        deliveryRoute.validateStatusTransition(command.getRouteStatus());

        // 담당자 상태 변경 준비
        DeliveryManager deliveryManager = null;
        if (command.getRouteStatus() == RouteStatus.HUB_IN_TRANSIT) {
            deliveryManager = validateStartConditions(deliveryRoute);
        }
        else if (deliveryRoute.getRouteStatus() == RouteStatus.HUB_IN_TRANSIT
                && (command.getRouteStatus() == RouteStatus.HUB_ARRIVED || command.getRouteStatus() == RouteStatus.FAILED)) {
            deliveryManager = findAssignedManagerForUpdate(deliveryRoute);
        }

        // 첫/마지막 경로 계산
        boolean isFirstRoute = deliveryRoute.getRouteSequence() == 1;
        boolean isLastRoute = command.getRouteStatus() == RouteStatus.HUB_ARRIVED &&
                !deliveryRouteRepositoryPort.existsByDeliveryIdAndRouteSequenceGreaterThan(delivery.getDeliveryId(), deliveryRoute.getRouteSequence());

        // 배송 경로 및 배송 상태 변경
        DeliveryStatus previousDeliveryStatus = delivery.getDeliveryStatus();
        LocalDateTime changedAt = LocalDateTime.now();
        deliveryRoute.updateStatus(command.getRouteStatus(), changedAt);
        updateDeliveryManagerStatus(deliveryManager, command.getRouteStatus());
        updateDeliveryStatus(delivery, command.getRouteStatus(), isFirstRoute, isLastRoute);

        // 배송 상태 변경 이벤트 발행
        if (previousDeliveryStatus != delivery.getDeliveryStatus()) {
            // TODO: 트랜잭션 커밋 후 DeliveryStatusChangedEvent 발행
        }

        // 변경사항 반영 및 응답 변환
        deliveryRouteRepositoryPort.flush();
        return new DeliveryRouteStatusResult(deliveryRoute);
    }

    private void validateUpdateAuthority(UpdateDeliveryRouteStatusCommand command, DeliveryRoute deliveryRoute) {
        if ("MASTER_ADMIN".equals(command.getUserRole())) {
            return;
        }
        if ("HUB_ADMIN".equals(command.getUserRole())) {
            boolean isRelatedHub = command.getAffiliationId() != null
                    && (Objects.equals(command.getAffiliationId(), deliveryRoute.getOriginHubId())
                    || Objects.equals(command.getAffiliationId(), deliveryRoute.getDestHubId()));
            if (isRelatedHub) {
                return;
            }
        }
        if ("DELIVERY_MANAGER".equals(command.getUserRole())) {
            boolean isAssignedManager = command.getLoginUserId() != null && deliveryRoute.getDeliveryManager() != null
                    && Objects.equals(command.getLoginUserId(), deliveryRoute.getDeliveryManager().getDeliveryManagerId());
            if (isAssignedManager) {
                return;
            }
        }

        throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_UPDATE_FORBIDDEN);
    }

    private void validateDeliveryStatus(Delivery delivery) {
        if (delivery.getDeliveryStatus() != DeliveryStatus.HUB_WAITING
                && delivery.getDeliveryStatus() != DeliveryStatus.HUB_IN_TRANSIT) {
            throw new BaseException(DeliveryErrorCode.INVALID_DELIVERY_ROUTE_STATUS_TRANSITION);
        }
    }

    private DeliveryManager validateStartConditions(DeliveryRoute deliveryRoute) {
        // 담당자 배정 검증
        if (deliveryRoute.getDeliveryManager() == null) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_NOT_ASSIGNED);
        }

        DeliveryManager deliveryManager = findAssignedManagerForUpdate(deliveryRoute);
        if (deliveryManager.getManagerType() != ManagerType.HUB_DELIVERY) {
            throw new BaseException(DeliveryErrorCode.INVALID_ASSIGNED_DELIVERY_MANAGER_TYPE);
        }
        if (deliveryManager.getManagerStatus() != ManagerStatus.AVAILABLE) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE);
        }

        // 첫 번째 경로 확인
        if (deliveryRoute.getRouteSequence() <= 1) {
            return deliveryManager;
        }

        // 이전 경로 완료 검증
        DeliveryRoute previousRoute = deliveryRouteRepositoryPort.findByDeliveryIdAndRouteSequence(
                        deliveryRoute.getDelivery().getDeliveryId(), deliveryRoute.getRouteSequence() - 1)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.PREVIOUS_DELIVERY_ROUTE_NOT_COMPLETED));

        if (previousRoute.getRouteStatus() != RouteStatus.HUB_ARRIVED) {
            throw new BaseException(DeliveryErrorCode.PREVIOUS_DELIVERY_ROUTE_NOT_COMPLETED);
        }

        return deliveryManager;
    }

    private DeliveryManager findAssignedManagerForUpdate(DeliveryRoute deliveryRoute) {
        if (deliveryRoute.getDeliveryManager() == null) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_NOT_ASSIGNED);
        }

        return deliveryManagerRepositoryPort.findByIdForUpdate(
                        deliveryRoute.getDeliveryManager().getDeliveryManagerId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_NOT_ASSIGNED));
    }

    private void updateDeliveryManagerStatus(DeliveryManager deliveryManager, RouteStatus routeStatus) {
        if (deliveryManager == null) {
            return;
        }
        if (routeStatus == RouteStatus.HUB_IN_TRANSIT) {
            deliveryManager.startDelivery();
            return;
        }
        if (routeStatus == RouteStatus.HUB_ARRIVED || routeStatus == RouteStatus.FAILED) {
            deliveryManager.finishDelivery();
        }
    }

    private void updateDeliveryStatus(Delivery delivery, RouteStatus routeStatus, boolean isFirstRoute, boolean isLastRoute) {
        // 경로 실패 전파
        if (routeStatus == RouteStatus.FAILED) {
            delivery.deliveryFailed();
            return;
        }

        // 첫 경로 이동 시작
        if (routeStatus == RouteStatus.HUB_IN_TRANSIT && isFirstRoute) {
            delivery.startHubTransit();
            return;
        }

        // 마지막 경로 도착
        if (routeStatus == RouteStatus.HUB_ARRIVED && isLastRoute) {
            delivery.arriveDestinationHub();
        }
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
