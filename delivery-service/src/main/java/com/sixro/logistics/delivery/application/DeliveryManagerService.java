package com.sixro.logistics.delivery.application;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.infrastructure.DeliveryManagerRepository;
import com.sixro.logistics.delivery.presentation.dto.ManagerCreateReqDto;
import com.sixro.logistics.delivery.presentation.dto.ManagerCreateResDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeliveryManagerService {
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
        // TODO: userservice 병합 시 코드 수정
        // 1. 유저테이블에 존재하고, 활성화된 유저인가?
        /*User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다."));*/

        // 2. 이미 manager 테이블에 존재하는가?
        if (managerRepository.existsById(userId)) {
            throw new IllegalArgumentException("이미 해당 배송 담당자가 존재합니다.");
        }

        // 3. 요청에 소속 허브가 있을 경우 해당 허브 존재여부 / 활성화여부 체크
        /*if (!hubRepository.existsById(hubId)) {
            throw new IllegalArgumentException("입력된 허브가 존재하지 않습니다.");
        }*/

        // TODO 4. 사용자 테이블에서의 ROLE이 배송담당자가 맞는지

//                - 순번계산하기
//                - 엔티티 생성하기

        return null;
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
}
