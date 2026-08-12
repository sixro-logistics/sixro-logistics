package com.sixro.logistics.notification.application.service;

import com.sixro.logistics.notification.domain.entity.AIRequestLog;
import com.sixro.logistics.notification.domain.entity.AIRequestStatus;
import com.sixro.logistics.notification.domain.event.DeliveryCreatedEvent;
import com.sixro.logistics.notification.domain.repository.AiRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String geminiUrl;

    private final RestClient restClient = RestClient.create();
    private final AiRequestLogRepository aiRequestLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Transactional
    public String generateSlackMessage(DeliveryCreatedEvent.DeliveryCreatedData data) {
        String prompt = buildPrompt(data);

        return callAiApiAndSaveLog(data.getOrderId(), prompt);
    }

    private String buildPrompt(DeliveryCreatedEvent.DeliveryCreatedData data) {
        String productsInfo = "정보 없음";

        if (!CollectionUtils.isEmpty(data.getProducts())) {
            productsInfo = data.getProducts().stream()
                    .map(p -> String.format("상품 ID(%s) %d개", p.getProductId(), p.getQuantity()))
                    .collect(Collectors.joining(", "));
        }

        String originHub = data.getOriginHubId() != null ? data.getOriginHubId().toString() : "미정";
        String routeHubs = "직송 (경유지 없음)";

        if (!CollectionUtils.isEmpty(data.getRoutes())) {
            routeHubs = data.getRoutes().stream()
                    .map(r -> r.getDestHubId().toString())
                    .collect(Collectors.joining(", "));
        }

        String managerInfo = getDeliveryManagerText(data);

        return String.format("""
                너는 물류 알림 매니저 AI야.
                전달받은 [원천 데이터]를 바탕으로 [Slack 메시지 템플릿]과 동일한 형식의 메시지를 생성해줘.

                [원천 데이터]
                - 주문 번호(orderId): %s
                - 주문/배송 일시(occurredAt/deadline): %s
                - 상품 정보: %s
                - 요청 사항: %s
                - 발송지(출발 허브): %s
                - 경유지: %s
                - 도착지(최종 배송지): %s
                - 배송 담당자: %s
                - 총 허브 이동 소요시간(초): %d

                [Slack 메시지 템플릿]
                주문 번호 : {orderId}
                주문자 정보 : {주문자 정보}
                주문 시간 : {주문 시간}
                상품 정보 : {상품 정보}
                요청 사항 : {요청 사항}
                발송지 : {발송지}
                경유지 : {경유지}
                도착지 : {도착지}
                배송담당자 : {배송담당자}

                위 내용을 기반으로 도출된 최종 발송 시한은 {AI가_계산한_최종_발송_시한} 입니다.

                [요구사항]
                1. 템플릿의 항목 레이아웃을 정확히 유지할 것.
                2. 배송 담당자가 지정되지 않은 경우 '배송담당자 : 미정' 형태로 출력할 것.
                3. 납품 기한 및 이동 소요 시간을 계산하여 '최종 발송 시한'을 자연스러운 한글 날짜/시간 형태로 도출해 포함할 것.
                """,
                data.getOrderId(),
                data.getDeliveryDeadline() != null ? data.getDeliveryDeadline().format(DATE_FORMATTER) : "정보 없음",
                productsInfo,
                data.getRequests() != null ? data.getRequests() : "없음",
                originHub,
                routeHubs,
                data.getDeliveryAddress() != null ? data.getDeliveryAddress() : "미정",
                managerInfo,
                data.getTotalHubRouteExpectedDurationS() != null ? data.getTotalHubRouteExpectedDurationS() : 0L
        );
    }

    private String getDeliveryManagerText(DeliveryCreatedEvent.DeliveryCreatedData data) {
        if (CollectionUtils.isEmpty(data.getDeliveryManagers())) {
            return "미정 (배송 담당자 지정 대기 중)";
        }

        return data.getDeliveryManagers().stream()
                .map(m -> {
                    String managerId = m.getDeliveryManagerId() != null ? m.getDeliveryManagerId().toString() : "ID 미정";
                    String type = m.getManagerType() != null ? m.getManagerType() : "유형 미정";
                    return String.format("%s (%s)", managerId, type);
                })
                .collect(Collectors.joining(" / "));
    }

    @SuppressWarnings("unchecked")
    private String callAiApiAndSaveLog(UUID orderId, String prompt) {
        try {
            log.info("[AI Service] Gemini Native REST API 호출 중...");

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            Map response = restClient.post()
                    .uri(geminiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String generatedContent = parseGeminiResponse(response);

            saveLog(orderId, prompt, generatedContent, AIRequestStatus.SUCCESS, null);

            return generatedContent;

        } catch (Exception e) {
            log.error("[AI Service] Gemini API 호출 실패", e);
            saveLog(orderId, prompt, null, AIRequestStatus.FAILED, e.getMessage());
            throw new RuntimeException("AI 메시지 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String parseGeminiResponse(Map response) {
        if (response == null || !response.containsKey("candidates")) {
            throw new IllegalStateException("Gemini API 응답 결과가 비어있습니다.");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        return (String) parts.get(0).get("text");
    }

    private void saveLog(UUID orderId, String requestData, String responseData, AIRequestStatus status, String errorMessage) {
        AIRequestLog aiLog = AIRequestLog.builder()
                .orderId(orderId)
                .requestData(requestData)
                .responseData(responseData)
                .requestStatus(status)
                .errorMessage(errorMessage)
                .createdBy(SYSTEM_USER_ID)
                .updatedBy(SYSTEM_USER_ID)
                .isDeleted(false)
                .build();

        aiRequestLogRepository.save(aiLog);
    }
}