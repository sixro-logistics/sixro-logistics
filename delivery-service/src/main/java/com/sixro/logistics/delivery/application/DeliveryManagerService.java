package com.sixro.logistics.delivery.application;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.infrastructure.DeliveryManagerRepository;
import com.sixro.logistics.delivery.presentation.dto.ManagerCreateReqDto;
import com.sixro.logistics.delivery.presentation.dto.ManagerCreateResDto;
import com.sixro.logistics.delivery.presentation.dto.ManagerInfoResDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeliveryManagerService {
    private static final int MAX_DELIVERY_MANAGER_COUNT = 10;
    private final DeliveryManagerRepository managerRepository;

    public DeliveryManagerService(DeliveryManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    public ManagerCreateResDto createDeliveryManager(String userRole, UUID affiliationId, ManagerCreateReqDto managerCreateReqDto) {
        UUID userId = managerCreateReqDto.getUserId();
        UUID hubId = managerCreateReqDto.getHubId();
        ManagerType managerType = managerCreateReqDto.getManagerType();

        // 현재 유저의 생성 권한 체크 TODO: UserRole Enum으로 권한 체크
        validateCreateAuthority(userRole, affiliationId, hubId, managerType);

        // dto 검증: 생성하려는 담당자 정보 확인
        // 0. 규칙에 맞는 요청값인지?
        validateManagerTypeAndHub(managerType, hubId);

        // 1. 유저테이블에 존재하고, 활성화된 유저인가?
            // TODO: 사용자 테이블에서의 ROLE이 배송담당자가 맞는지 user openfeign 단건 조회

        // 2. DeliveryManager 중복 검증
        if (managerRepository.existsById(userId)) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_ALREADY_EXISTS);
        }

        // 3. 요청에 소속 허브가 있을 경우 해당 허브 존재 / 활성화여부 체크
            // TODO: hub openfeign 단건 조회

        // 순번 계산
        int deliverySequence = assignDeliverySequence(managerType, hubId);

        // 엔티티 생성
        DeliveryManager deliveryManager = DeliveryManager.create(userId, hubId, managerType, deliverySequence);
        DeliveryManager savedManager = managerRepository.save(deliveryManager);
        return new ManagerCreateResDto(savedManager);
    }

    private void validateCreateAuthority(String userRole, UUID affiliationId, UUID hubId, ManagerType managerType) {
        if ("MASTER_ADMIN".equals(userRole)) {
            return;
        }
        if (!"HUB_ADMIN".equals(userRole)) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_FORBIDDEN);
        }
        if (affiliationId == null || !affiliationId.equals(hubId)) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_FORBIDDEN);
        }
        if (managerType.equals(ManagerType.HUB_DELIVERY)) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_FORBIDDEN);
        }
    }

    private void validateManagerTypeAndHub(ManagerType managerType, UUID hubId) {
        if (managerType == ManagerType.HUB_DELIVERY && hubId != null) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_HUB_MISMATCH);
        }

        if (managerType == ManagerType.COMPANY_DELIVERY && hubId == null) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_HUB_MISMATCH);
        }
    }

    private int assignDeliverySequence(ManagerType managerType, UUID hubId) {
        long activeManagerCount;
        int maxSequence;

        if (managerType == ManagerType.HUB_DELIVERY) {
            activeManagerCount = managerRepository.countByManagerTypeAndHubIdIsNull(ManagerType.HUB_DELIVERY);
            maxSequence = managerRepository.findMaxHubDeliverySequenceIncludingDeleted()
                    .orElse(-1);
        }
        else {
            activeManagerCount = managerRepository.countByManagerTypeAndHubId(ManagerType.COMPANY_DELIVERY, hubId);
            maxSequence = managerRepository.findMaxCompanyDeliverySequenceIncludingDeleted(hubId)
                    .orElse(-1);
        }

        if (activeManagerCount >= MAX_DELIVERY_MANAGER_COUNT) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_CAPACITY_EXCEEDED);
        }

        return maxSequence + 1;
    }

    @Transactional(readOnly = true)
    public ManagerInfoResDto getDeliveryManager(UUID loginUserId, String userRole, UUID affiliationId, UUID deliveryManagerId) {

        // 담당자 조회
        DeliveryManager deliveryManager = managerRepository.findById(deliveryManagerId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        // 현재 유저의 조회 권한 검증
        validateGetAuthority(loginUserId, userRole, affiliationId, deliveryManager);

        return new ManagerInfoResDto(deliveryManager);
    }

    private void validateGetAuthority(UUID loginUserId, String userRole, UUID affiliationId, DeliveryManager deliveryManager) { // TODO: UserRole
        ManagerType targetManagerType = deliveryManager.getManagerType();
        UUID hubId = deliveryManager.getHubId();
        UUID managerId = deliveryManager.getDeliveryManagerId();

        if ("MASTER_ADMIN".equals(userRole)) { // 모두 허용
            return;
        }
        // hub admin + company delivery + 현재 로그인한 유저의 허브와 배송담당자의 허브가 같음 -> 정상
        if ("HUB_ADMIN".equals(userRole) && targetManagerType == ManagerType.COMPANY_DELIVERY
                && affiliationId!=null && affiliationId.equals(hubId)) {
            return;
        }
        if ("HUB_ADMIN".equals(userRole)) { // 나머지 경우의 hub_admin -> 차단
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_FORBIDDEN);
        }
        if ("DELIVERY_MANAGER".equals(userRole) && loginUserId.equals(managerId)) { // -> 정상
            return;
        }
        // 나머지 모두 차단
        throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_FORBIDDEN);
    }
}
