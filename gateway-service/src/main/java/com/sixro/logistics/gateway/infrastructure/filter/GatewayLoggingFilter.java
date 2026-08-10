package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.HeaderConstants;
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
 * <p>Request ID, HTTP Method, Path, 응답 상태 및 처리 시간을 기록하며,
 * Authorization Header와 요청·응답 Body 등 민감 정보는 기록하지 않습니다.</p>
 */
@Slf4j
@Component
public class GatewayLoggingFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst(HeaderConstants.REQUEST_ID);

        long startedAt = System.currentTimeMillis();

        log.info(
                "[Gateway Request] requestId={}, method={}, path={}, remoteAddress={}",
                requestId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getRemoteAddress()
        );

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long durationMs =
                            System.currentTimeMillis() - startedAt;

                    log.info(
                            "[Gateway Response] requestId={}, method={}, path={}, status={}, durationMs={}",
                            requestId,
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI().getPath(),
                            exchange.getResponse().getStatusCode(),
                            durationMs
                    );
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}