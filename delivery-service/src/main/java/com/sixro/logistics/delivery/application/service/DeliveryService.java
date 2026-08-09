package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.util.PageUtil;
import com.sixro.logistics.delivery.application.command.GetDeliveryCommand;
import com.sixro.logistics.delivery.application.command.SearchDeliveriesCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryManagerCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryStatusCommand;
import com.sixro.logistics.delivery.application.result.DeliveryManagerAssignmentResult;
import com.sixro.logistics.delivery.application.result.DeliverySearchResult;
import com.sixro.logistics.delivery.application.result.DeliveryResult;
import com.sixro.logistics.delivery.application.result.DeliveryStatusResult;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import com.sixro.logistics.delivery.domain.DeliverySearchCondition;
import com.sixro.logistics.delivery.domain.enums.DeliverySearchScope;
import com.sixro.logistics.delivery.domain.port.DeliveryManagerRepositoryPort;
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
public class DeliveryService {

    private final DeliveryRepositoryPort deliveryRepositoryPort;
    private final DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;
    private final DeliveryManagerRepositoryPort deliveryManagerRepositoryPort;

    public DeliveryService(DeliveryRepositoryPort deliveryRepositoryPort, DeliveryRouteRepositoryPort deliveryRouteRepositoryPort,
                           DeliveryManagerRepositoryPort deliveryManagerRepositoryPort) {
        this.deliveryRepositoryPort = deliveryRepositoryPort;
        this.deliveryRouteRepositoryPort = deliveryRouteRepositoryPort;
        this.deliveryManagerRepositoryPort = deliveryManagerRepositoryPort;
    }

    @Transactional(readOnly = true)
    public DeliveryResult getDelivery(GetDeliveryCommand command) {
        Delivery delivery = deliveryRepositoryPort.findById(command.getDeliveryId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        validateGetAuthority(command, delivery);

        return new DeliveryResult(delivery);
    }

    public DeliveryStatusResult updateDeliveryStatus(UpdateDeliveryStatusCommand command) {
        // 배송 조회
        Delivery delivery = deliveryRepositoryPort.findByIdForUpdate(command.getDeliveryId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        // 변경 권한 검증
        validateUpdateAuthority(command, delivery);

        // 역할별 변경 범위 검증
        validateRoleStatusAuthority(command);

        // 동일 상태 요청 처리
        DeliveryStatus previousStatus = delivery.getDeliveryStatus();
        if (previousStatus == command.getDeliveryStatus()) {
            return new DeliveryStatusResult(delivery, previousStatus, delivery.getUpdatedAt(), delivery.getUpdatedBy());
        }

        // 직접 변경 가능 상태 검증
        validateDirectUpdateStatus(command.getDeliveryStatus());

        // 업체 배송 담당자 상태 변경 준비
        DeliveryManager deliveryManager = prepareCompanyDeliveryManagerStatusChange(
                delivery, previousStatus, command.getDeliveryStatus());

        // 배송 상태 변경
        LocalDateTime changedAt = LocalDateTime.now();
        delivery.updateStatus(command.getDeliveryStatus());
        updateCompanyDeliveryManagerStatus(deliveryManager, command.getDeliveryStatus());

        // 배송 취소 전파
        if (command.getDeliveryStatus() == DeliveryStatus.CANCELLED) {
            List<DeliveryRoute> waitingRoutes = deliveryRouteRepositoryPort.findAllWaitingByDeliveryId(delivery.getDeliveryId());
            waitingRoutes.forEach(DeliveryRoute::cancelByDelivery);
        }

        // 배송 상태 변경 이벤트 발행
        // TODO: 트랜잭션 커밋 후 DeliveryStatusChangedEvent 발행

        // 변경사항 반영 및 응답 변환
        deliveryRepositoryPort.flush();
        return new DeliveryStatusResult(delivery, previousStatus, changedAt, command.getLoginUserId());
    }

    public DeliveryManagerAssignmentResult updateDeliveryManager(UpdateDeliveryManagerCommand command) {
        // 배송 잠금 조회
        Delivery delivery = deliveryRepositoryPort.findByIdForUpdate(command.getDeliveryId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        // 담당자 배정 권한 검증
        validateDeliveryManagerUpdateAuthority(command, delivery);

        // 동일 담당자 요청 처리
        UUID previousDeliveryManagerId = delivery.getDeliveryManager() == null
                ? null : delivery.getDeliveryManager().getDeliveryManagerId();
        if (Objects.equals(previousDeliveryManagerId, command.getDeliveryManagerId())) {
            return new DeliveryManagerAssignmentResult(delivery, previousDeliveryManagerId);
        }

        // 배송 상태 검증
        validateDeliveryManagerUpdateStatus(delivery);

        // 배송 담당자 잠금 조회 및 검증
        DeliveryManager deliveryManager = deliveryManagerRepositoryPort.findByIdForUpdate(command.getDeliveryManagerId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));
        validateAssignableCompanyDeliveryManager(delivery, deliveryManager);

        // 업체 배송 담당자 배정 및 응답 변환
        delivery.assignDeliveryManager(deliveryManager);
        deliveryRepositoryPort.flush();
        return new DeliveryManagerAssignmentResult(delivery, previousDeliveryManagerId);
    }

    private void validateDeliveryManagerUpdateAuthority(UpdateDeliveryManagerCommand command, Delivery delivery) {
        if ("MASTER_ADMIN".equals(command.getUserRole())) {
            return;
        }
        if ("HUB_ADMIN".equals(command.getUserRole()) && command.getAffiliationId() != null
                && Objects.equals(command.getAffiliationId(), delivery.getDestHubId())) {
            return;
        }

        throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_ASSIGNMENT_FORBIDDEN);
    }

    private void validateDeliveryManagerUpdateStatus(Delivery delivery) {
        DeliveryStatus deliveryStatus = delivery.getDeliveryStatus();
        boolean isAssignableStatus = deliveryStatus == DeliveryStatus.HUB_WAITING
                || deliveryStatus == DeliveryStatus.HUB_IN_TRANSIT
                || deliveryStatus == DeliveryStatus.DESTINATION_HUB_ARRIVED;

        if (!isAssignableStatus) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_ASSIGNMENT_NOT_ALLOWED);
        }
    }

    private void validateAssignableCompanyDeliveryManager(Delivery delivery, DeliveryManager deliveryManager) {
        if (deliveryManager.getManagerType() != ManagerType.COMPANY_DELIVERY) {
            throw new BaseException(DeliveryErrorCode.INVALID_ASSIGNED_DELIVERY_MANAGER_TYPE);
        }
        if (!Objects.equals(deliveryManager.getHubId(), delivery.getDestHubId())) {
            throw new BaseException(DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_HUB_MISMATCH);
        }
        if (deliveryManager.getManagerStatus() == ManagerStatus.OFF_DUTY) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_ASSIGNABLE);
        }
    }

    private void validateUpdateAuthority(UpdateDeliveryStatusCommand command, Delivery delivery) {
        if ("MASTER_ADMIN".equals(command.getUserRole())) {
            return;
        }
        if ("HUB_ADMIN".equals(command.getUserRole())) {
            boolean isRelatedHub = command.getAffiliationId() != null
                    && (Objects.equals(command.getAffiliationId(), delivery.getOriginHubId())
                    || Objects.equals(command.getAffiliationId(), delivery.getDestHubId()));
            if (isRelatedHub) {
                return;
            }
        }
        if ("DELIVERY_MANAGER".equals(command.getUserRole())) {
            DeliveryManager deliveryManager = delivery.getDeliveryManager();
            if (deliveryManager == null || deliveryManager.getManagerType() != ManagerType.COMPANY_DELIVERY) {
                throw new BaseException(DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED);
            }
            if (Objects.equals(command.getLoginUserId(), deliveryManager.getDeliveryManagerId())) {
                return;
            }
        }

        throw new BaseException(DeliveryErrorCode.DELIVERY_STATUS_UPDATE_FORBIDDEN);
    }

    private void validateDirectUpdateStatus(DeliveryStatus deliveryStatus) {
        if (deliveryStatus == DeliveryStatus.HUB_WAITING || deliveryStatus == DeliveryStatus.HUB_IN_TRANSIT
                || deliveryStatus == DeliveryStatus.DESTINATION_HUB_ARRIVED) {
            throw new BaseException(DeliveryErrorCode.INVALID_DELIVERY_STATUS_TRANSITION);
        }
    }

    private void validateRoleStatusAuthority(UpdateDeliveryStatusCommand command) {
        DeliveryStatus deliveryStatus = command.getDeliveryStatus();

        if ("MASTER_ADMIN".equals(command.getUserRole())) {
            return;
        }
        if ("HUB_ADMIN".equals(command.getUserRole()) && deliveryStatus != DeliveryStatus.CANCELLED) {
            return;
        }
        if ("DELIVERY_MANAGER".equals(command.getUserRole())
                && (deliveryStatus == DeliveryStatus.COMPANY_DELIVERY_IN_PROGRESS
                || deliveryStatus == DeliveryStatus.DELIVERED)) {
            return;
        }

        throw new BaseException(DeliveryErrorCode.DELIVERY_STATUS_UPDATE_FORBIDDEN);
    }

    private DeliveryManager prepareCompanyDeliveryManagerStatusChange(Delivery delivery, DeliveryStatus previousStatus, DeliveryStatus targetStatus) {
        // 업체 배송 시작/종료 여부 계산
        boolean startsCompanyDelivery = targetStatus == DeliveryStatus.COMPANY_DELIVERY_IN_PROGRESS;
        boolean finishesCompanyDelivery = previousStatus == DeliveryStatus.COMPANY_DELIVERY_IN_PROGRESS
                && (targetStatus == DeliveryStatus.DELIVERED || targetStatus == DeliveryStatus.FAILED);

        // 담당자 상태 연동 대상 확인
        if (!startsCompanyDelivery && !finishesCompanyDelivery) {
            return null;
        }

        // 배정 담당자 존재 검증
        DeliveryManager assignedManager = delivery.getDeliveryManager();
        if (assignedManager == null) {
            throw new BaseException(DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED);
        }

        // 배정 담당자 잠금 조회
        DeliveryManager deliveryManager = deliveryManagerRepositoryPort
                .findByIdForUpdate(assignedManager.getDeliveryManagerId())
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED));

        // 업체 배송 담당자 유형 검증
        if (deliveryManager.getManagerType() != ManagerType.COMPANY_DELIVERY) {
            throw new BaseException(DeliveryErrorCode.INVALID_ASSIGNED_DELIVERY_MANAGER_TYPE);
        }

        // 목적지 허브 소속 검증
        if (!Objects.equals(deliveryManager.getHubId(), delivery.getDestHubId())) {
            throw new BaseException(DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_HUB_MISMATCH);
        }
        return deliveryManager;
    }

    private void updateCompanyDeliveryManagerStatus(DeliveryManager deliveryManager, DeliveryStatus targetStatus) {
        if (deliveryManager == null) {
            return;
        }

        // 업체 배송 시작 처리
        if (targetStatus == DeliveryStatus.COMPANY_DELIVERY_IN_PROGRESS) {
            deliveryManager.startDelivery();
        }
        else if (targetStatus == DeliveryStatus.DELIVERED || targetStatus == DeliveryStatus.FAILED) {
            // 업체 배송 종료 처리
            deliveryManager.finishDelivery();
        }
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
            // TODO: OrderConfirmedEvent를 소비: 공급업체/수령업체 ID가 포함된 Delivery 생성이 되었음
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
