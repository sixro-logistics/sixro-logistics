package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.HeaderConstants;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Gateway를 통과하는 요청과 응답의 주요 정보를 기록합니다.
 *
 * <p>RequestId는 개별 요청 식별에 사용하고,
 * TraceId는 MSA 서비스 간 요청 흐름 추적에 사용합니다.</p>
 *
 * <p>Authorization Header와 요청·응답 Body 등
 * 민감 정보는 로그에 기록하지 않습니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayLoggingFilter implements WebFilter, Ordered {

    private final Tracer tracer;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst(HeaderConstants.REQUEST_ID);

        long startedAt = System.currentTimeMillis();

        /*
         * WebFlux에서는 Tracing Context가 Reactor Context를 통해 전달되므로
         * 실제 reactive chain 실행 시점에 현재 Span을 조회합니다.
         */
        return Mono.defer(() -> {

            String traceId = resolveTraceId();

            log.info(
                    "[Gateway Request] requestId={}, traceId={}, method={}, path={}, remoteAddress={}",
                    requestId,
                    traceId,
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    exchange.getRequest().getRemoteAddress()
            );

            return chain.filter(exchange)
                    .doFinally(signalType -> {

                        long durationMs =
                                System.currentTimeMillis() - startedAt;

                        /*
                         * 응답 완료 시점에도 현재 Trace 정보를 다시 조회합니다.
                         *
                         * 응답 처리 과정에서 Thread가 변경될 수 있으므로
                         * filter() 진입 시 저장한 값을 무조건 재사용하지 않습니다.
                         */
                        String responseTraceId =
                                resolveTraceId();

                        log.info(
                                "[Gateway Response] requestId={}, traceId={}, method={}, path={}, status={}, durationMs={}",
                                requestId,
                                responseTraceId,
                                exchange.getRequest().getMethod(),
                                exchange.getRequest().getURI().getPath(),
                                exchange.getResponse().getStatusCode(),
                                durationMs
                        );
                    });
        });
    }

    /**
     * Micrometer Tracing에서 현재 요청의 Trace ID를 조회합니다.
     */
    private String resolveTraceId() {
        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {
            return null;
        }

        return currentSpan.context().traceId();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}