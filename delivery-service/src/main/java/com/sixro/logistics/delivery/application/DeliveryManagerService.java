package com.sixro.logistics.delivery.application;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.infrastructure.DeliveryManagerRepository;
import com.sixro.logistics.delivery.presentation.dto.ManagerCreateReqDto;
import com.sixro.logistics.delivery.presentation.dto.ManagerCreateResDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DeliveryManagerService {
    private static final int MAX_DELIVERY_MANAGER_COUNT = 10;
    private final DeliveryManagerRepository managerRepository;

    public DeliveryManagerService(DeliveryManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    @Transactional
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
            throw new IllegalArgumentException("이미 해당 배송 담당자가 존재합니다.");
        }

        // 3. 요청에 소속 허브가 있을 경우 해당 허브 존재 / 활성화여부 체크
            // TODO: hub openfeign 단건 조회


        // 순번 계산하기
        int deliverySequence = assignDeliverySequence(managerType, hubId);

        // 엔티티 생성하기
        DeliveryManager deliveryManager = DeliveryManager.create(userId, hubId, managerType, deliverySequence);
        DeliveryManager savedManager = managerRepository.save(deliveryManager);
        return new ManagerCreateResDto(savedManager);
    }

    private void validateCreateAuthority(String userRole, UUID affiliationId, UUID hubId, ManagerType managerType) {
        if ("MASTER_ADMIN".equals(userRole)) {
            return;
        }
        if (!"HUB_ADMIN".equals(userRole)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
        if (affiliationId == null || !affiliationId.equals(hubId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
        if (managerType.equals(ManagerType.HUB_DELIVERY)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateManagerTypeAndHub(ManagerType managerType, UUID hubId) {
        if (managerType == ManagerType.HUB_DELIVERY && hubId != null) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        if (managerType == ManagerType.COMPANY_DELIVERY && hubId == null) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private int assignDeliverySequence(ManagerType managerType, UUID hubId) {
        List<Integer> usedSequences;

        if (managerType == ManagerType.HUB_DELIVERY) {
            usedSequences = managerRepository.findHubDeliveryUsedSequences(ManagerType.HUB_DELIVERY);
        }
        else {
            usedSequences = managerRepository.findCompanyDeliveryUsedSequences(ManagerType.COMPANY_DELIVERY, hubId);
        }

        Set<Integer> usedSequenceSet = new HashSet<>(usedSequences);

        for (int sequence = 1; sequence <= MAX_DELIVERY_MANAGER_COUNT; sequence++) {
            if (!usedSequenceSet.contains(sequence)) {
                return sequence;
            }
        }
        throw new BaseException(CommonErrorCode.CONFLICT);
    }
}
