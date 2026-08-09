package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.delivery.application.command.GetDeliveryRouteCommand;
import com.sixro.logistics.delivery.application.result.DeliveryRouteResult;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
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
}
