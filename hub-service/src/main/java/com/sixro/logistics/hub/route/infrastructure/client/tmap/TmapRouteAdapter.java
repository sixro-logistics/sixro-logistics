package com.sixro.logistics.hub.route.infrastructure.client.tmap;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.application.port.ExternalRoutePort;
import com.sixro.logistics.hub.route.application.port.RouteSearchCondition;
import com.sixro.logistics.hub.route.application.port.RouteSnapshotResult;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmapRouteAdapter implements ExternalRoutePort {

    private final TmapRouteClient tmapRouteClient;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    @RateLimiter(name = "tmapApi", fallbackMethod = "fallbackRateLimit")
    @CircuitBreaker(name = "tmapApi", fallbackMethod = "fallbackCircuitBreaker")
    @Retry(name = "tmapApi", fallbackMethod = "fallbackRetry")
    public RouteSnapshotResult getRouteSnapshot(RouteSearchCondition condition) {

        long startTime = System.currentTimeMillis();
        log.info("[TMAP API Request] 경로 탐색 시작. 옵션: totalValue={}", condition.totalValue());

        TmapRouteRequest request = TmapRouteRequest.builder()
                .startX(condition.startX())
                .startY(condition.startY())
                .endX(condition.endX())
                .endY(condition.endY())
                .totalValue(condition.totalValue())
                .trafficInfo(condition.trafficInfo())
                .build();

        TmapRouteResponse response;

        try {
            response = tmapRouteClient.getRouteInfo(TmapRouteClient.DEFAULT_VERSION, request);

        } catch (HttpClientErrorException e) {
            // 4xx 에러 처리
            log.error("[TMAP API 4xx Error] 호출 실패. 상태코드: {}, 본문: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new BaseException(HubRouteErrorCode.EXTERNAL_API_UNAUTHORIZED);
            }
            throw new BaseException(HubRouteErrorCode.EXTERNAL_API_BAD_REQUEST);

        } catch (HttpServerErrorException e) {
            // 5xx 에러 처리
            log.warn("[TMAP API 5xx Error] 서버 장애 발생. 재시도를 트리거합니다. 상태코드: {}, 본문: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e; // Retry가 감지할 수 있도록 예외 재전파
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[TMAP API Response] 호출 성공. 소요시간: {}ms", duration);

        return parseToDomainDto(response, condition);
    }

    // --- Fallback Methods ---

    // Retry 최종 실패 시 발생하는 예외
    private RouteSnapshotResult fallbackRetry(RouteSearchCondition condition, Exception e) {
        log.error("[TMAP API 재시도 최종 실패] 3회 시도 초과. Cause: {}", e.getMessage());
        throw new BaseException(HubRouteErrorCode.EXTERNAL_API_ERROR); // 500
    }

    // 서킷 오픈 시 차단 예외
    private RouteSnapshotResult fallbackCircuitBreaker(RouteSearchCondition condition, CallNotPermittedException e) {
        log.error("[TMAP API 서킷 오픈] 외부 시스템 장애로 요청 차단됨.");
        throw new BaseException(HubRouteErrorCode.EXTERNAL_API_UNAVAILABLE); // 503
    }

    // Rate Limit 초과 시 예외
    private RouteSnapshotResult fallbackRateLimit(RouteSearchCondition condition, RequestNotPermitted e) {
        log.error("[TMAP API Rate Limit 초과] AppKey 초당 호출 제한 도달.");
        throw new BaseException(HubRouteErrorCode.EXTERNAL_API_RATE_LIMIT_EXCEEDED); // 429
    }

    // --- Parsing ---

    @SuppressWarnings("unchecked")
    private RouteSnapshotResult parseToDomainDto(TmapRouteResponse response, RouteSearchCondition condition) {
        int totalDistance = 0, totalTime = 0, totalFare = 0;
        List<Coordinate> pathCoordinates = new ArrayList<>();

        if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
            log.error("[TMAP API Parsing Error] 유효한 Feature 데이터가 없습니다.");
            throw new BaseException(HubRouteErrorCode.EXTERNAL_API_ERROR);
        }

        for (TmapRouteResponse.Feature feature : response.getFeatures()) {
            TmapRouteResponse.Properties props = feature.getProperties();
            TmapRouteResponse.Geometry geom = feature.getGeometry();

            if (props != null) {
                if (props.getTotalDistance() != null && props.getTotalDistance() > 0) {
                    totalDistance = props.getTotalDistance();
                }
                if (props.getTotalTime() != null && props.getTotalTime() > 0) {
                    totalTime = props.getTotalTime();
                }
                if (props.getTotalFare() != null) {
                    totalFare = props.getTotalFare();
                }
            }

            // totalValue가 1(모든 정보)일 때만 LineString 파싱 수행
            if (condition.totalValue() == 1 && geom != null && "LineString".equals(geom.getType())) {
                List<List<Number>> lineCoords = (List<List<Number>>) geom.getCoordinates();
                for (List<Number> coord : lineCoords) {
                    pathCoordinates.add(new Coordinate(coord.get(0).doubleValue(), coord.get(1).doubleValue()));
                }
            }
        }

        LineString routePath = null;
        if (!pathCoordinates.isEmpty()) {
            routePath = geometryFactory.createLineString(pathCoordinates.toArray(new Coordinate[0]));
            routePath.setSRID(4326);
        }

        // 수정 필요, app 계층 코드 새로 만들고 나서 작업 해야함
        return RouteSnapshotResult.builder()
                .distance(totalDistance)
                .duration(totalTime)
                .tollFee(totalFare)
                .routePath(routePath)
                .build();
    }
}