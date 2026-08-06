package com.sixro.logistics.inventory.infrastructure.client.hub;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.model.HubInfo;
import com.sixro.logistics.inventory.application.port.HubQueryPort;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
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
            HubClientResponse response = hubClient.getHub(hubId);

            return new HubInfo(
                    response.hubId()/*,
                    response.hubStatus()*/
            );

        } catch (FeignException.NotFound e) {
            throw new BaseException(InventoryErrorCode.HUB_NOT_FOUND);
        }/* catch (FeignException e) {
            throw new BaseException(InventoryErrorCode.HUB_SERVICE_UNAVAILABLE);
        }*/

    }
}