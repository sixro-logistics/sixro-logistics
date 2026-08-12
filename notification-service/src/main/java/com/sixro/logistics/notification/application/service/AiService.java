package com.sixro.logistics.notification.application.service;

import com.sixro.logistics.notification.presentation.dto.request.SlackMessageCreateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient chatClient;

    public String calculateDeadline(SlackMessageCreateDto.OrderInfo order) {
        String prompt = String.format("""
                다음 물류 이동 정보를 기반으로 배송 담당자 근무시간(09:00~18:00)과 이동 경로를 고려하여 납기일자에 맞추기 위한 '최종 발송 시한'을 계산해 주세요.
                답변은 정확한 최종 발송 시한 날짜와 시간만 명확히 한 문장으로 제시해 주세요. (예: 12월 10일 오전 9시)

                - 상품 및 수량: %s %d박스
                - 주문 요청 사항: %s
                - 발송지: %s
                - 경유지: %s
                - 도착지: %s
                - 배송 담당자 근무시간: 09:00 - 18:00
                """,
                order.getProductName(), order.getQuantity(), order.getRequirement(),
                order.getOrigin(), order.getStopovers(), order.getDestination()
        );

        return chatClient.prompt().user(prompt).call().content();
    }
}
