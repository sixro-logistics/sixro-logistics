package com.sixro.logistics.hub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.common.test.config.KafkaTestContainerConfig;
import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import com.sixro.logistics.common.test.config.RedisTestContainerConfig;
import com.sixro.logistics.hub.hub.domain.model.Address;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubZone;
import com.sixro.logistics.hub.hub.domain.model.Location;
import com.sixro.logistics.hub.hub.infrastructure.persistence.command.HubJpaRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers({
        PostgresTestContainerConfig.class,
        RedisTestContainerConfig.class,
        KafkaTestContainerConfig.class
})
@Transactional
class HubFullIntegrationTest {

    private static final String HUB_TOPIC = "isolated-hub-full-topic";

    @Autowired
    private HubJpaRepository hubJpaRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HubKafkaConsumer hubKafkaConsumer;

    @Test
    @DisplayName("Hub 데이터를 PostGIS 저장, Redis 캐싱, Kafka 이벤트 발행을 모두 성공적으로 수행한다")
    void shouldIntegrateHubDataAcrossPostgisRedisAndKafka() throws Exception {
        // given
        Hub hub = Hub.builder()
                .hubName("서울특별시 센터")
                .address(Address.of("05838", "서울특별시 송파구 송파대로 55", "서울특별시 송파구 장지동 862", "동남권물류단지 A동"))
                .location(Location.of(127.1249, 37.4776))
                .hubZone(HubZone.CAPITAL)
                .maxCapacity(1_000_000)
                .build();

        String hubEventPayload = """
            {
              "hubName": "서울특별시 센터",
              "zipcode": "05838",
              "roadAddress": "서울특별시 송파구 송파대로 55",
              "jibunAddress": "서울특별시 송파구 장지동 862",
              "detailAddress": "동남권물류단지 A동",
              "longitude": 127.1249,
              "latitude": 37.4776,
              "hubZone": "CAPITAL",
              "maxCapacity": 1000000
            }
            """;

        // when
        Hub savedHub = hubJpaRepository.save(hub);
        hubJpaRepository.flush();

        String cacheKey = "hub:cache:" + savedHub.getHubName();
        redisTemplate.opsForValue().set(cacheKey, hubEventPayload);

        kafkaTemplate.send(HUB_TOPIC, savedHub.getHubName(), hubEventPayload);

        // then
        Hub dbResult = hubJpaRepository.findById(savedHub.getId()).orElseThrow();
        assertThat(dbResult.getHubName()).isEqualTo("서울특별시 센터");

        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedData).isNotNull();

        String receivedKafkaMessage = hubKafkaConsumer.getLatestMessage(10, TimeUnit.SECONDS);
        assertThat(receivedKafkaMessage).isNotNull();
        assertThat(objectMapper.readTree(receivedKafkaMessage).get("hubZone").asText()).isEqualTo("CAPITAL");
    }

    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        public HubKafkaConsumer hubKafkaConsumer() {
            return new HubKafkaConsumer();
        }
    }

    static class HubKafkaConsumer {
        private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        @KafkaListener(topics = HUB_TOPIC, groupId = "isolated-hub-full-group")
        public void listen(String message) {
            messageQueue.add(message);
        }

        public String getLatestMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return messageQueue.poll(timeout, unit);
        }
    }
}