package com.sixro.logistics.hub.route.presentation.api;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.hub.route.application.query.OptimalPathResult;
import com.sixro.logistics.hub.route.application.usecase.FindOptimalPathService;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;
import com.sixro.logistics.hub.route.presentation.request.SearchOptimalPathRequest;
import com.sixro.logistics.hub.route.presentation.response.OptimalPathResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/hub-routes")
@RequiredArgsConstructor
@Validated
public class HubRouteInternalController {

    private final FindOptimalPathService findOptimalPathService;

    @PostMapping("/paths")
    public CommonResponse<OptimalPathResponse> searchOptimalPath(
            @RequestParam(required = false, defaultValue = "OPTIMAL") PathSearchType searchType,
            @Valid @RequestBody SearchOptimalPathRequest request
    ) {
        OptimalPathResult result = findOptimalPathService.searchOptimalPath(
                request.originHubId(),
                request.destinationHubId(),
                searchType
        );
        OptimalPathResponse response = OptimalPathResponse.from(result);

        return CommonResponse.success("최적 경로 탐색을 완료했습니다.", response);
    }
}