package com.sixro.logistics.delivery.infrastructure.client.hubroute;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.model.HubRoutePathInfo;
import com.sixro.logistics.delivery.application.model.HubRouteProductInfo;
import com.sixro.logistics.delivery.application.port.HubRouteQueryPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubRouteClientAdapter implements HubRouteQueryPort {

    private final HubRouteClient hubRouteClient;

    @Override
    public Optional<HubRoutePathInfo> findPath(UUID originHubId, UUID destHubId, List<HubRouteProductInfo> products) {
        try {
            CommonResponse<HubRouteClientResponse> response = hubRouteClient.getPath(
                    originHubId,
                    destHubId,
                    HubRouteClientRequest.from(products)
            );

            if (response == null || !response.success() || response.data() == null || response.data().routes() == null) {
                throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR, new IllegalStateException("hub-service가 유효하지 않은 경로 응답을 반환했습니다."));
            }

            HubRouteClientResponse data = response.data();

            if (!originHubId.equals(data.originHubId()) || !destHubId.equals(data.destHubId())) {
                throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR, new IllegalStateException("hub-service의 경로 응답 허브가 요청과 일치하지 않습니다."));
            }

            List<HubRoutePathInfo.RouteInfo> routes = data.routes().stream()
                    .map(route -> new HubRoutePathInfo.RouteInfo(
                            route.hubRouteId(),
                            route.sequence(),
                            route.originHubId(),
                            route.destHubId(),
                            route.expectedDistanceM(),
                            route.expectedDurationS()
                    ))
                    .toList();

            return Optional.of(new HubRoutePathInfo(data.originHubId(), data.destHubId(), routes));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }
}
