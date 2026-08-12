package com.sixro.logistics.hub.hub.presentation.api;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.hub.hub.application.query.HubInternalInfo;
import com.sixro.logistics.hub.hub.application.query.HubInternalQueryService;
import com.sixro.logistics.hub.hub.presentation.response.HubInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/hubs")
@RequiredArgsConstructor
@Validated
public class HubInternalController {

    private final HubInternalQueryService hubInternalQueryService;

    @GetMapping("/{hub_id}")
    public CommonResponse<HubInternalResponse> getInternalOperatingHub(
            @PathVariable("hub_id") UUID hubId
    ) {
        HubInternalInfo info = hubInternalQueryService.getOperatingHub(hubId);
        HubInternalResponse response = HubInternalResponse.from(info);

        return CommonResponse.success("허브 조회에 성공했습니다.", response);
    }
}