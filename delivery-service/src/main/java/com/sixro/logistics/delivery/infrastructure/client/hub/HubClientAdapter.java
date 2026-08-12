package com.sixro.logistics.delivery.infrastructure.client.hub;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.model.HubInfo;
import com.sixro.logistics.delivery.application.port.HubQueryPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubClientAdapter implements HubQueryPort {

    private final HubClient hubClient;

    @Override
    public Optional<HubInfo> findHub(UUID hubId) {
        try {
            CommonResponse<HubClientResponse> response = hubClient.getHub(hubId);

            if (response == null || !response.success() || response.data() == null) {
                throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR, new IllegalStateException("hub-service가 유효하지 않은 응답을 반환했습니다."));
            }

            HubClientResponse data = response.data();

            return Optional.of(new HubInfo(data.hubId(), data.hubName()));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }
}
