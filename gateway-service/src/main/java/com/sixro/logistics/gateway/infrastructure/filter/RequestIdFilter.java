package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.HeaderConstants;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Gateway로 들어오는 요청에 고유 Request ID를 부여합니다.
 *
 * <p>클라이언트가 전달한 기존 Request ID는 신뢰하지 않고 제거한 뒤,
 * Gateway에서 새로 생성한 값을 내부 서비스와 응답 Header에 전달합니다.</p>
 */
@Component
public class RequestIdFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String requestId = UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(HeaderConstants.REQUEST_ID);
                    headers.set(
                            HeaderConstants.REQUEST_ID,
                            requestId
                    );
                })
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request)
                .build();

        mutatedExchange.getResponse()
                .getHeaders()
                .set(
                        HeaderConstants.REQUEST_ID,
                        requestId
                );

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}