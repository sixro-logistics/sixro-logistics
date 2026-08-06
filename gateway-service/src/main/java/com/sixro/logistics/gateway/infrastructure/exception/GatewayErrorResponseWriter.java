package com.sixro.logistics.gateway.infrastructure.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * <p>Spring Security(WebFlux) 필터에서 발생하는 예외는
 * ControllerAdvice를 거치지 않으므로 직접 응답을 생성합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class GatewayErrorResponseWriter {

    private final ObjectMapper objectMapper;

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

        SerializedError serializedError = serialize(errorCode);

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
     * ErrorCode를 공통 ErrorResponse(JSON)로 직렬화합니다.
     *
     * <p>직렬화에 실패하면 내부 서버 오류 응답으로 대체합니다.</p>
     */
    private SerializedError serialize(ErrorCode errorCode) {
        try {
            byte[] responseBody = objectMapper.writeValueAsBytes(
                    ErrorResponse.from(errorCode)
            );

            return new SerializedError(errorCode, responseBody);

        } catch (JsonProcessingException exception) {
            // ErrorResponse 생성 실패 시 500 응답으로 대체합니다.
            return serializeFallback();
        }
    }

    /**
     * ErrorResponse 직렬화 실패 시 사용할
     * 최종 예외 응답을 생성합니다.
     */
    private SerializedError serializeFallback() {
        ErrorCode fallbackErrorCode =
                CommonErrorCode.INTERNAL_SERVER_ERROR;

        try {
            byte[] responseBody = objectMapper.writeValueAsBytes(
                    ErrorResponse.from(fallbackErrorCode)
            );

            return new SerializedError(
                    fallbackErrorCode,
                    responseBody
            );

        } catch (JsonProcessingException exception) {
            /*
             * ObjectMapper 자체가 동작하지 않는 예외적인 상황입니다.
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