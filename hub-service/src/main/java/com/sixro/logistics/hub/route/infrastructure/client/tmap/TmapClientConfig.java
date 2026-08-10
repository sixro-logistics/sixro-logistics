package com.sixro.logistics.hub.route.infrastructure.client.tmap;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class TmapClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Value("${tmap.route-base-url}")
    private String tmapRouteBaseUrl;

    @Value("${tmap.app-key}")
    private String appKey;

    @Bean
    public TmapRouteClient tmapRouteClient() {
        // HttpClient 생성하여 Connect Timeout 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        // 요청 팩토리에 Timeout 정책 적용
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        // RestClient 생성
        RestClient restClient = RestClient.builder()
                .baseUrl(tmapRouteBaseUrl)
                .defaultHeader("appKey", appKey)
                .requestFactory(requestFactory)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(TmapRouteClient.class);    }
}