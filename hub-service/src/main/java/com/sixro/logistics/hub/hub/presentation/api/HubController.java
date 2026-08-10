package com.sixro.logistics.hub.hub.presentation.api;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.common.core.util.PageUtil;
import com.sixro.logistics.hub.hub.application.command.HubCommandService;
import com.sixro.logistics.hub.hub.application.query.HubQueryService;
import com.sixro.logistics.hub.hub.domain.model.HubZone;
import com.sixro.logistics.hub.hub.application.auth.UserContext;
import com.sixro.logistics.hub.common.auth.RequireRole;
import com.sixro.logistics.hub.hub.presentation.dto.HubDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
@Validated
public class HubController {

    private final HubCommandService hubCommandService;
    private final HubQueryService hubQueryService;

    @PostMapping
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<HubDto.Response> createHub(
            @Valid @RequestBody HubDto.CreateRequest request
    ) {
        UUID hubId = hubCommandService.createHub(request.toCommand());
        HubDto.Response response = hubQueryService.getHub(hubId);
        return CommonResponse.created("허브가 성공적으로 등록되었습니다.", response);
    }

    @GetMapping("/{hub_id}")
    public CommonResponse<HubDto.Response> getHub(
            @PathVariable("hub_id") UUID hubId
    ) {
        HubDto.Response response = hubQueryService.getHub(hubId);
        return CommonResponse.success("허브 상세 조회를 성공했습니다.", response);
    }

    @GetMapping
    public CommonResponse<PageResponse<HubDto.Response>> searchHubs(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(required = false, defaultValue = "createdAt") String sortField,
            @RequestParam(required = false) HubZone hubZone,
            @RequestParam(required = false) String hubName
    ) {
        int springPage = Math.max(0, page - 1);

        Pageable pageable = PageUtil.toPageable(springPage, size, direction, sortField);

        PageResponse<HubDto.Response> response = hubQueryService.searchHubs(hubZone, hubName, pageable);
        return CommonResponse.success("허브 목록 조회를 성공했습니다.", response);
    }

    @PutMapping("/{hub_id}")
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<HubDto.Response> updateHub(
            @PathVariable("hub_id") UUID hubId,
            @Valid @RequestBody HubDto.UpdateRequest request
    ) {
        UUID updatedHubId = hubCommandService.updateHub(hubId, request.toCommand());
        HubDto.Response response = hubQueryService.getHub(updatedHubId);
        return CommonResponse.success("허브 정보가 수정되었습니다.", response);
    }

    @PatchMapping("/{hub_id}/status")
    @RequireRole({"MASTER_ADMIN", "HUB_ADMIN"})
    public CommonResponse<HubDto.Response> changeHubStatus(
            @PathVariable("hub_id") UUID hubId,
            @Valid @RequestBody HubDto.StatusUpdateRequest request,
            UserContext userContext
    ) {
        UUID updatedHubId = hubCommandService.changeHubStatus(hubId, request.toCommand(), userContext);
        HubDto.Response response = hubQueryService.getHub(updatedHubId);
        return CommonResponse.success("허브 상태가 변경되었습니다.", response);
    }

    @DeleteMapping("/{hub_id}")
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<Void> deleteHub(
            @PathVariable("hub_id") UUID hubId,
            UserContext userContext) {
        hubCommandService.deleteHub(hubId, userContext);
        return CommonResponse.success("허브가 성공적으로 삭제되었습니다.");
    }

    @GetMapping("/search/nearest")
    public CommonResponse<HubDto.NearestResponse> getNearestHub(
            // 대한민국 영토 Bounding Box (경도 124~132, 위도 33~39 (제주도 포함))
            @RequestParam("longitude")
            @Min(value = 124, message = "유효한 대한민국 경도 범위를 벗어났습니다.")
            @Max(value = 132, message = "유효한 대한민국 경도 범위를 벗어났습니다.") double longitude,

            @RequestParam("latitude")
            @Min(value = 33, message = "유효한 대한민국 위도 범위를 벗어났습니다.")
            @Max(value = 39, message = "유효한 대한민국 위도 범위를 벗어났습니다.") double latitude
    ) {
        HubDto.NearestResponse response = hubQueryService.getNearestHub(longitude, latitude);
        return CommonResponse.success("가장 가까운 활성 허브를 찾았습니다.", response);
    }
}