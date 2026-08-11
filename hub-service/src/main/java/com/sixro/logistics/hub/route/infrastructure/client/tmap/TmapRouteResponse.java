package com.sixro.logistics.hub.route.infrastructure.client.tmap;

import lombok.Getter;
import java.util.List;

@Getter
public class TmapRouteResponse {

    private String type;
    private List<Feature> features;

    @Getter
    public static class Feature {
        private String type;
        private Geometry geometry;
        private Properties properties;
    }

    @Getter
    public static class Geometry {
        // type 종류는 Point 또는 LineString
        private String type;
        // 좌표 값이 1차원 배열(Point)과 2차원 배열(LineString)이 혼재하므로 Object로 매핑 후 안전하게 캐스팅하여 사용
        private Object coordinates;
    }

    @Getter
    public static class Properties {
        private Integer totalDistance;
        private Integer totalTime;
        private Integer totalFare;
        private String pointType;
    }
}