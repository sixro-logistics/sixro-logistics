package com.sixro.logistics.order.infrastructure.client.hub;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.order.application.model.HubInfo;
import com.sixro.logistics.order.application.port.HubQueryPort;
import com.sixro.logistics.order.exception.OrderErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubClientAdapter implements HubQueryPort {

    private final HubClient hubClient;

    @Override
    public HubInfo getHub(UUID hubId) {

        try {
            CommonResponse<HubClientResponse> response = hubClient.getHub(hubId);

            return new HubInfo(
                    response.data().hubId()
            );

        } catch (FeignException.NotFound e) {
            throw new BaseException(OrderErrorCode.HUB_NOT_FOUND);
        }

    }
}