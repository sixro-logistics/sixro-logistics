package com.sixro.logistics.hub.route.infrastructure.client.tmap;

import lombok.Builder;
import lombok.Getter;


/**
 * TMAP 자동차 경로 안내 API 요청을 위한 인프라 DTO.
 * - 프로젝트에서 사용하는 옵션만 작성
 * - 상세한 API 스펙과 추가 옵션은 아래 SK Open API 문서 참고
 *
 * @see <a href="https://openapi.sk.com/products/detail?svcSeq=4&menuSeq=46#Request_%EC%83%81%EC%84%B8_%EC%84%A4%EB%AA%85_">SK Open API 상세 설명</a>
 */
@Getter
@Builder
public class TmapRouteRequest {
    /**
     * <h3>출발지 X 좌표</h3>
     * <p>출발 허브의 경도 (longitude)</p>
     */
    private double startX;

    /**
     * <h3>출발지 Y 좌표</h3>
     * <p>출발 허브의 위도 (latitude)</p>
     */
    private double startY;

    /**
     * <h3>목적지 X 좌표</h3>
     * <p>도착 허브의 경도 (longitude)</p>
     */
    private double endX;

    /**
     * <h3>목적지 Y 좌표</h3>
     * <p>도착 허브의 위도 (latitude)</p>
     */
    private double endY;

    /**
     * <h3>요청 좌표계 지정</h3>
     * <p>WGS84 좌표계 사용 고정</p>
     */
    @Builder.Default
    private String reqCoordType = "WGS84GEO";

    /**
     * <h3>응답 좌표계 지정</h3>
     * <p>WGS84 좌표계 사용 고정</p>
     */
    @Builder.Default
    private String resCoordType = "WGS84GEO";

    /**
     * <h3>경로 탐색 옵션</h3>
     * <h4>옵션</h4>
     * <ul>
     *     <li><b>0</b>: 교통최적 + 추천</li>
     *     <li><b>4</b>: 교통최적 + 고속도로우선</li>
     * </ul>
     * <h4>사용 가이드</h4>
     * <ul>
     *     <li>간선 화물 이동이므로 고속도로 우선인 <b>4</b> 사용</li>
     *     <li>사용하는 서비스에 따라 '0' 사용</li>
     * </ul>
     */
    @Builder.Default
    private int searchOption = 4;

    /**
     * <h3>톨게이트 요금 산정 차종</h3>
     * <h4>옵션</h4>
     * <ul>
     *     <li><b>1</b>: 승용차</li>
     *     <li><b>4</b>: 대형화물차</li>
     *     <li><b>5</b>: 특수화물차</li>
     * </ul>
     * <h4>사용 가이드</h4>
     * <ul>
     *     <li>B2B 물류 시스템의 트레일러/윙바디를 가정하여 <b>4</b> 사용</li>
     * </ul>
     */
    @Builder.Default
    private int carType = 4;

    /**
     * <h3>요금 가중치 옵션</h3>
     * <h4>옵션</h4>
     * <ul>
     *     <li><b>1</b>: 유료/무료</li>
     *     <li><b>2</b>: 최적 요금</li>
     *     <li><b>8</b>: 무료 우선</li>
     *     <li><b>16</b>: 로직 판단</li>
     * </ul>
     * <h4>사용 가이드</h4>
     * <ul>
     *     <li>TMAP 알고리즘 기반 최적 밸런스(시간/비용)를 찾기 위해 <b>16</b> 사용</li>
     * </ul>
     */
    @Builder.Default
    private int tollgateFareOption = 16;

    /**
     * <h3>응답 데이터 상세 수준 (totalValue)</h3>
     * <h4>옵션</h4>
     * <ul>
     *     <li><b>1</b>: 검색 결과의 모든 정보 반환</li>
     *     <li><b>2</b>: 검색 결과 중 totalDistance, totalTime, totalFare, taxiFare 정보만 반환</li>
     * </ul>
     * <h4>사용 가이드</h4>
     * <ul>
     *     <li>경로 데이터 생성에는 routePath 저장을 위해 <b>1</b></b> 사용</li>
     *     <li>일부 데이터 갱신에는 '2' 사용</li>
     * </ul>
     */
    @Builder.Default
    private int totalValue = 1;

    /**
     * <h3>교통 정보 포함 여부 (trafficInfo)</h3>
     * <p>외부 API 호출 시 교통 정보를 포함할지 여부를 결정합니다.</p>
     * <h4>옵션</h4>
     * <ul>
     *     <li><b>Y</b>: 교통 정보 포함</li>
     *     <li><b>N</b>: 교통 정보 미포함</li>
     * </ul>
     * <h4>사용 가이드</h4>
     * <ul>
     *     <li>경로 데이터 관리 기능에는 <b>N</b> 사용</li>
     *     <li>사용하는 서비스에 따라 'Y' 사용</li>
     * </ul>
     */
    @Builder.Default
    private String trafficInfo = "N";
}