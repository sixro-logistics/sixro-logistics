package com.sixro.logistics.delivery.application;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.delivery.application.command.GetDeliveryCommand;
import com.sixro.logistics.delivery.application.result.DeliveryResult;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
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
}
