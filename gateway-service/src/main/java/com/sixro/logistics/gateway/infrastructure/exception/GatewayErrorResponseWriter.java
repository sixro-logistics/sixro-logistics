package com.sixro.logistics.gateway.infrastructure.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.exception.ErrorCode;
import com.sixro.logistics.common.core.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway에서 발생한 인증·인가 오류를
 * 공통 ErrorResponse(JSON) 형식으로 작성하는 컴포넌트입니다.
 *
 * <p>ControllerAdvice를 거치지 않는 Gateway 보안 오류이므로
 * 응답 상태 코드와 JSON Body를 직접 생성합니다.</p>
 *
 * <p>RequestIdFilter에서 생성한 X-Request-Id를
 * 오류 응답 Body의 requestId에도 함께 포함하여
 * 요청 로그와 오류 응답을 동일한 식별자로 추적할 수 있도록 합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class GatewayErrorResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * Gateway 보안 오류를 공통 ErrorResponse 형식으로 작성합니다.
     */
    public Mono<Void> write(
            ServerWebExchange exchange,
            ErrorCode errorCode
    ) {
        /*
         * 이미 응답이 전송된 경우
         * 추가로 ErrorResponse를 작성하지 않습니다.
         */
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        String requestId =
                resolveRequestId(exchange);

        SerializedError serializedError = serialize(errorCode, requestId);

        exchange.getResponse()
                .setStatusCode(serializedError.errorCode().getStatus());

        exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        DataBuffer dataBuffer = exchange.getResponse()
                .bufferFactory()
                .wrap(serializedError.responseBody());

        return exchange.getResponse()
                .writeWith(Mono.just(dataBuffer));
    }

    /**
     * RequestIdFilter에서 Gateway 요청에 설정한
     * X-Request-Id를 조회합니다.
     *
     * <p>일반적으로 Request Header에서 조회되며,
     * 예외적인 경우를 대비하여 Response Header도 확인합니다.</p>
     */
    private String resolveRequestId(
            ServerWebExchange exchange
    ) {
        String requestId =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                HeaderConstants.REQUEST_ID
                        );

        if (requestId != null
                && !requestId.isBlank()) {
            return requestId;
        }

        return exchange.getResponse()
                .getHeaders()
                .getFirst(
                        HeaderConstants.REQUEST_ID
                );
    }

    /**
     * ErrorCode와 Request ID를
     * 공통 ErrorResponse(JSON)로 직렬화합니다.
     */
    private SerializedError serialize(
            ErrorCode errorCode,
            String requestId
    ) {
        try {
            byte[] responseBody =
                    objectMapper.writeValueAsBytes(ErrorResponse.from(errorCode, requestId));

            return new SerializedError(errorCode, responseBody);

        } catch (JsonProcessingException exception) {

            /*
             * ErrorResponse 직렬화 실패 시에도
             * 가능한 경우 동일한 Request ID를 유지하여
             * 오류 추적이 가능하도록 합니다.
             */
            return serializeFallback(requestId);
        }
    }

    /**
     * ErrorResponse 직렬화 실패 시 사용할
     * 최종 예외 응답을 생성합니다.
     */
    private SerializedError serializeFallback(String requestId) {
        ErrorCode fallbackErrorCode =
                CommonErrorCode.INTERNAL_SERVER_ERROR;

        try {
            byte[] responseBody = objectMapper.writeValueAsBytes(
                    ErrorResponse.from(fallbackErrorCode, requestId)
            );

            return new SerializedError(
                    fallbackErrorCode,
                    responseBody
            );

        } catch (JsonProcessingException exception) {
            /*
             * ObjectMapper 자체가 동작하지 않는 극히 예외적인 상황입니다.
             * 이 경우 JSON Body는 생성할 수 없으므로
             * 빈 응답 본문과 함께 500 상태코드를 반환합니다.
             */
            return new SerializedError(
                    fallbackErrorCode,
                    new byte[0]
            );
        }
    }

    /**
     * HTTP 상태 코드와 직렬화된 응답 본문을 함께 보관합니다.
     */
    private record SerializedError(
            ErrorCode errorCode,
            byte[] responseBody
    ) {
    }
}