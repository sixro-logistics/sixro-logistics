package com.sixro.logistics.hub.route.presentation.api;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.common.core.util.PageUtil;
import com.sixro.logistics.hub.common.auth.RequireRole;
import com.sixro.logistics.hub.common.auth.UserContext;
import com.sixro.logistics.hub.route.application.command.HubRouteBatchService;
import com.sixro.logistics.hub.route.application.command.HubRouteCommandService;
import com.sixro.logistics.hub.route.application.command.HubRouteInitService;
import com.sixro.logistics.hub.route.application.query.HubRouteDetailInfo;
import com.sixro.logistics.hub.route.application.query.HubRouteInfo;
import com.sixro.logistics.hub.route.application.query.HubRouteQueryService;
import com.sixro.logistics.hub.route.presentation.request.CreateHubRouteRequest;
import com.sixro.logistics.hub.route.presentation.request.SyncHubRouteRequest;
import com.sixro.logistics.hub.route.presentation.request.UpdateHubRouteRequest;
import com.sixro.logistics.hub.route.presentation.response.HubRouteDetailResponse;
import com.sixro.logistics.hub.route.presentation.response.HubRouteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hub-routes")
@RequiredArgsConstructor
@Validated
public class HubRouteController {

    private final HubRouteCommandService hubRouteCommandService;
    private final HubRouteQueryService hubRouteQueryService;
    private final HubRouteBatchService hubRouteBatchService;
    private final HubRouteInitService hubRouteInitService;

    // --- Command APIs ---

    @PostMapping
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<HubRouteDetailResponse> createRoute(
            @Valid @RequestBody CreateHubRouteRequest request
    ) {
        UUID hubRouteId = hubRouteCommandService.createRoute(request.toCommand());
        HubRouteDetailInfo info = hubRouteQueryService.getRoute(hubRouteId);
        HubRouteDetailResponse response = HubRouteDetailResponse.from(info);

        return CommonResponse.created("허브 노선이 성공적으로 생성되었습니다.", response);
    }

    @PutMapping("/{hub_route_id}")
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<HubRouteDetailResponse> updateRoute(
            @PathVariable("hub_route_id") UUID hubRouteId,
            @Valid @RequestBody UpdateHubRouteRequest request
    ) {
        UUID updatedHubRouteId = hubRouteCommandService.updateRoute(hubRouteId, request.toCommand());
        HubRouteDetailInfo info = hubRouteQueryService.getRoute(updatedHubRouteId);
        HubRouteDetailResponse response = HubRouteDetailResponse.from(info);

        return CommonResponse.success("허브 노선 정보가 수정되었습니다.", response);
    }

    @DeleteMapping("/{hub_route_id}")
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<Void> deleteRoute(
            @PathVariable("hub_route_id") UUID hubRouteId,
            UserContext userContext
    ) {
        hubRouteCommandService.deleteRoute(hubRouteId, userContext.userId());

        return CommonResponse.success("허브 노선이 성공적으로 삭제되었습니다.");
    }

    @PatchMapping("/{hub_route_id}/sync")
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<HubRouteDetailResponse> syncRoute(
            @PathVariable("hub_route_id") UUID hubRouteId,
            @RequestBody(required = false) SyncHubRouteRequest request
    ) {

        SyncHubRouteRequest syncRequest = request != null ? request : new SyncHubRouteRequest(null, null);

        UUID syncedHubRouteId = hubRouteCommandService.syncRoute(hubRouteId, syncRequest.toCommand());

        HubRouteDetailInfo info = hubRouteQueryService.getRoute(syncedHubRouteId);
        HubRouteDetailResponse response = HubRouteDetailResponse.from(info);

        return CommonResponse.success("허브 노선 단건 동기화가 완료되었습니다.", response);
    }

    // --- Batch & Init APIs ---

    @PostMapping("/init")
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<Void> initializeRoutes() {
        hubRouteInitService.initializeRoutesBackground();

        return CommonResponse.accepted("허브망 초기화 작업이 백그라운드에서 시작되었습니다.",null);
    }

    @PostMapping("/sync-all")
    @RequireRole({"MASTER_ADMIN"})
    public CommonResponse<Void> syncAllRoutes() {
        hubRouteBatchService.syncAllRoutesBackground();

        return CommonResponse.accepted("전체 노선 동기화 작업이 백그라운드에서 시작되었습니다.", null);
    }

    // --- Query APIs ---

    @GetMapping("/{hub_route_id}")
    public CommonResponse<HubRouteDetailResponse> getRoute(
            @PathVariable("hub_route_id") UUID hubRouteId
    ) {
        HubRouteDetailInfo info = hubRouteQueryService.getRoute(hubRouteId);
        HubRouteDetailResponse response = HubRouteDetailResponse.from(info);

        return CommonResponse.success("허브 노선 상세 조회를 성공했습니다.", response);
    }

    @GetMapping
    public CommonResponse<PageResponse<HubRouteResponse>> searchRoutes(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(required = false, defaultValue = "createdAt") String sortField,
            @RequestParam(required = false) UUID originHubId,
            @RequestParam(required = false) UUID destinationHubId
    ) {
        int springPage = Math.max(0, page - 1);

        Pageable pageable = PageUtil.toPageable(springPage, size, direction, sortField);
        Page<HubRouteInfo> infoPage = hubRouteQueryService.searchRoutes(originHubId, destinationHubId, pageable);
        PageResponse<HubRouteResponse> response = PageResponse.from(
                infoPage,
                HubRouteResponse::from
        );

        return CommonResponse.success("허브 노선 목록 조회를 성공했습니다.", response);
    }
}