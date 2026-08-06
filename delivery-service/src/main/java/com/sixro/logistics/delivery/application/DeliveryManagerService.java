package com.sixro.logistics.delivery.application;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.infrastructure.DeliveryManagerRepository;
import com.sixro.logistics.delivery.infrastructure.DeliveryRepository;
import com.sixro.logistics.delivery.infrastructure.DeliveryRouteRepository;
import com.sixro.logistics.delivery.presentation.dto.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class DeliveryManagerService {
    private static final int MAX_DELIVERY_MANAGER_COUNT = 10;
    private final DeliveryManagerRepository managerRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRepository deliveryRouteRepository;

    public DeliveryManagerService(DeliveryManagerRepository managerRepository, DeliveryRepository deliveryRepository, DeliveryRouteRepository deliveryRouteRepository) {
        this.managerRepository = managerRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryRouteRepository = deliveryRouteRepository;
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

    public ManagerUpdateResDto updateDeliveryManager(String userRole, UUID affiliationId, UUID deliveryManagerId, @Valid ManagerUpdateReqDto managerUpdateReqDto) {

        // 수정 대상 배송 담당자 조회
        DeliveryManager deliveryManager = managerRepository.findById(deliveryManagerId)
                .orElseThrow(() -> new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        // 변경 전 담당자 정보를 기준으로 요청자의 수정 권한 검증
        validateUpdateAuthority(userRole, affiliationId, deliveryManager);

        // 변경할 필드 존재 여부 검증
        UUID rhubId = managerUpdateReqDto.getHubId();
        ManagerType rmanagerType = managerUpdateReqDto.getManagerType();
        ManagerStatus rmanagerStatus = managerUpdateReqDto.getManagerStatus();

        if (rhubId == null && rmanagerType == null && rmanagerStatus == null) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        // 현재 배송 중인 담당자의 수정 요청은 모두 차단
        if (deliveryManager.getManagerStatus().equals(ManagerStatus.IN_DELIVERY)) {
            throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_UPDATE_NOT_ALLOWED);
        }

        // IN_DELIVERY 상태를 직접 지정하는 요청 차단
        if (rmanagerStatus == ManagerStatus.IN_DELIVERY)
            throw new BaseException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_STATUS_TRANSITION);


        ManagerType fmanagerType = (rmanagerType == null) ? deliveryManager.getManagerType() : rmanagerType;

        // 최종 담당자 유형 기준 유형-허브 조합 검증
        UUID fhubId;
        if (fmanagerType.equals(ManagerType.HUB_DELIVERY)){ // hub는 소속허브 없어야 함
            if (rhubId != null)
                throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_HUB_MISMATCH);
            fhubId = null;
        }
        else { // f=ManagerType.COMPANY_DELIVERY. company는 무조건 허브가 있어야 함
            fhubId = (rhubId != null) ? rhubId : deliveryManager.getHubId();
            if (fhubId==null)
                throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_HUB_MISMATCH);
        }

        // HUB_ADMIN: 변경 후 정보도 수정 가능 범위인지 검증
        if ("HUB_ADMIN".equals(userRole)) {
            if (fmanagerType != ManagerType.COMPANY_DELIVERY
                    || affiliationId == null || !affiliationId.equals(fhubId)) {
                throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_FORBIDDEN);
            }
        }

        ManagerStatus fmanagerStatus = (rmanagerStatus == null) ? deliveryManager.getManagerStatus() : rmanagerStatus;

        // 타입/허브변경 여부 체크
        boolean managerGroupChanged =
                (deliveryManager.getManagerType() != fmanagerType) || (!Objects.equals(deliveryManager.getHubId(), fhubId));

        // 그룹 유지: 기존 순번, 변경: 대상 그룹 마지막 순번
        int fdeliverySequence = deliveryManager.getDeliverySequence();

        if (managerGroupChanged) {
            // 해당 담당자 미완료 업무 존재여부 검증
            if (deliveryManager.getManagerType() == ManagerType.COMPANY_DELIVERY) {
                List<DeliveryStatus> completedStatuses = List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED, DeliveryStatus.FAILED);
                if (deliveryRepository.existsByDeliveryManager_DeliveryManagerIdAndDeliveryStatusNotIn(deliveryManagerId, completedStatuses)) {
                    throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_UPDATE_NOT_ALLOWED);
                }
            }
            else { // ManagerType.HUB_DELIVERY
                List<RouteStatus> completedStatuses = List.of(RouteStatus.HUB_ARRIVED, RouteStatus.CANCELLED, RouteStatus.FAILED);
                if (deliveryRouteRepository.existsByDeliveryManager_DeliveryManagerIdAndRouteStatusNotIn(deliveryManagerId, completedStatuses)) {
                    throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_UPDATE_NOT_ALLOWED);
                }
            }

            // TODO: company_delivery의 허브 변경하는 경우 허브 검증 (서비스 간 통신): 허브 단건 조회 api
            if (fmanagerType == ManagerType.COMPANY_DELIVERY) {
                //
            }

            // 순번 재지정
            fdeliverySequence = assignDeliverySequence(fmanagerType, fhubId);
        }

        // 최종값 엔티티 반영
        deliveryManager.update(fhubId, fmanagerType, fmanagerStatus, fdeliverySequence);

        // 수정 시간 반영용
        managerRepository.flush();

        return new ManagerUpdateResDto(deliveryManager);
    }

    private void validateUpdateAuthority(String userRole, UUID affiliationId, DeliveryManager deliveryManager) { // TODO: UserRole
        ManagerType targetManagerType = deliveryManager.getManagerType();
        UUID hubId = deliveryManager.getHubId();

        if ("MASTER_ADMIN".equals(userRole)) { // 모두 허용
            return;
        }
        // hub admin + company delivery + 현재 로그인한 유저의 허브와 배송담당자의 허브가 같음 -> 정상
        if ("HUB_ADMIN".equals(userRole) && targetManagerType == ManagerType.COMPANY_DELIVERY
                && affiliationId!=null && affiliationId.equals(hubId)) {
            return;
        }
        // 나머지 모두 차단
        throw new BaseException(DeliveryErrorCode.DELIVERY_MANAGER_FORBIDDEN);
    }
}
