package com.sixro.logistics.delivery.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.common.test.config.KafkaTestContainerConfig;
import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import com.sixro.logistics.delivery.application.model.CompanyHubInfo;
import com.sixro.logistics.delivery.application.model.HubRoutePathInfo;
import com.sixro.logistics.delivery.application.model.UserInfo;
import com.sixro.logistics.delivery.application.port.CompanyQueryPort;
import com.sixro.logistics.delivery.application.port.HubRouteQueryPort;
import com.sixro.logistics.delivery.application.port.UserQueryPort;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxEventType;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedData;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedEvent;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedItem;
import com.sixro.logistics.delivery.infrastructure.kafka.outbox.OutboxPublisher;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryManagerRepository;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRepository;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRouteRepository;
import com.sixro.logistics.delivery.infrastructure.repository.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@ActiveProfiles("test")
@ImportTestcontainers({
        PostgresTestContainerConfig.class,
        KafkaTestContainerConfig.class
})
class OrderCreatedEventConsumptionIntegrationTest {

    private static final String TRACE_ID = "order-created-integration-trace-id";
    private static final UUID EVENT_ID = UUID.fromString("01000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_ID = UUID.fromString("12000000-0000-0000-0000-000000000001");
    private static final UUID ORIGIN_HUB_ID = UUID.fromString("21000000-0000-0000-0000-000000000001");
    private static final UUID MIDDLE_HUB_ID = UUID.fromString("21000000-0000-0000-0000-000000000002");
    private static final UUID DEST_HUB_ID = UUID.fromString("21000000-0000-0000-0000-000000000003");
    private static final UUID SUPPLIER_COMPANY_ID = UUID.fromString("31000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_COMPANY_ID = UUID.fromString("31000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000001");

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private DeliveryManagerRepository deliveryManagerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private DeliveryRouteRepository deliveryRouteRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserQueryPort userQueryPort;

    @MockitoBean
    private CompanyQueryPort companyQueryPort;

    @MockitoBean
    private HubRouteQueryPort hubRouteQueryPort;

    // 이번 테스트에서는 Outbox를 Kafka로 발행하지 않고 PENDING 저장까지만 검증합니다.
    @MockitoBean
    private OutboxPublisher outboxPublisher;

    @Test
    @DisplayName("order.created 이벤트를 소비해 배송과 경로 및 PENDING Outbox를 저장한다")
    void consumeOrderCreatedEvent_createsDeliveryRoutesAndPendingOutbox() throws Exception {
        // given
        saveAvailableManagers();
        stubExternalServiceResponses();
        OrderCreatedEvent event = createOrderCreatedEvent();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaTopics.ORDER_CREATED, ORDER_ID.toString(), event);
        record.headers().add(new RecordHeader("trace-id", TRACE_ID.getBytes(StandardCharsets.UTF_8)));

        // when
        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);

        // Kafka Listener의 비동기 처리가 DB에 반영될 때까지 대기
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(deliveryRepository.findByOrderId(ORDER_ID)).isPresent();
            assertThat(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).hasSize(1);
        });

        // then
        Delivery delivery = deliveryRepository.findByOrderId(ORDER_ID).orElseThrow();
        assertThat(delivery.getOriginHubId()).isEqualTo(ORIGIN_HUB_ID);
        assertThat(delivery.getDestHubId()).isEqualTo(DEST_HUB_ID);
        assertThat(delivery.getRecipientCompanyId()).isEqualTo(RECEIVER_COMPANY_ID);
        assertThat(delivery.getRecipientName()).isEqualTo("홍길동");
        assertThat(delivery.getRecipientSlackId()).isEqualTo("U-RECIPIENT");

        List<UUID> supplierCompanyIds = jdbcTemplate.queryForList(
                "select supplier_company_id from delivery_schema.p_delivery_supplier_company "
                        + "where delivery_id = ?",
                UUID.class,
                delivery.getDeliveryId());
        assertThat(supplierCompanyIds).containsExactly(SUPPLIER_COMPANY_ID);

        // 조회한 외부 정보를 바탕으로 경로와 담당자 배정이 실제 저장됐는지 확인
        List<DeliveryRoute> routes = deliveryRouteRepository
                .findAllByDelivery_DeliveryIdOrderByRouteSequenceAsc(delivery.getDeliveryId());
        assertThat(routes).hasSize(2);
        assertThat(routes).extracting(DeliveryRoute::getRouteSequence).containsExactly(1, 2);
        assertThat(routes).extracting(DeliveryRoute::getExpectedDurationS).containsExactly(5_000L, 6_000L);

        List<Integer> assignedHubManagerSequences = jdbcTemplate.queryForList(
                "select dm.delivery_sequence from delivery_schema.p_delivery_route dr "
                        + "join delivery_schema.p_delivery_manager dm "
                        + "on dm.delivery_manager_id = dr.delivery_manager_id "
                        + "where dr.delivery_id = ? order by dr.route_sequence asc",
                Integer.class,
                delivery.getDeliveryId());
        assertThat(assignedHubManagerSequences).containsExactly(1, 2);

        Integer assignedCompanyManagerSequence = jdbcTemplate.queryForObject(
                "select dm.delivery_sequence from delivery_schema.p_delivery d "
                        + "join delivery_schema.p_delivery_manager dm "
                        + "on dm.delivery_manager_id = d.delivery_manager_id "
                        + "where d.delivery_id = ?",
                Integer.class,
                delivery.getDeliveryId());
        assertThat(assignedCompanyManagerSequence).isEqualTo(1);

        // 원본 trace-id와 주문 데이터가 DeliveryCreatedEvent Outbox까지 전달됐는지 확인
        Outbox outbox = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).getFirst();
        assertThat(outbox.getEventType()).isEqualTo(OutboxEventType.DELIVERY_CREATED);
        assertThat(outbox.getAggregateId()).isEqualTo(delivery.getDeliveryId());
        assertThat(outbox.getTraceId()).isEqualTo(TRACE_ID);

        JsonNode payload = objectMapper.readTree(outbox.getPayload());
        assertThat(payload.path("data").path("orderId").asText()).isEqualTo(ORDER_ID.toString());
        assertThat(payload.path("data").path("products").get(0).path("productId").asText())
                .isEqualTo(PRODUCT_ID.toString());
        assertThat(payload.path("data").path("products").get(0).path("quantity").asInt()).isEqualTo(4);
        assertThat(payload.path("data").path("routes")).hasSize(2);

        verify(userQueryPort).findUser(RECEIVER_ID);
        verify(companyQueryPort).findHubInfo(RECEIVER_COMPANY_ID);
        verify(hubRouteQueryPort).findPath(
                org.mockito.ArgumentMatchers.eq(ORIGIN_HUB_ID),
                org.mockito.ArgumentMatchers.eq(DEST_HUB_ID),
                anyList());
    }

    private void saveAvailableManagers() {
        DeliveryManager hubManager1 = DeliveryManager.create(
                UUID.randomUUID(), null, ManagerType.HUB_DELIVERY, 1);
        DeliveryManager hubManager2 = DeliveryManager.create(
                UUID.randomUUID(), null, ManagerType.HUB_DELIVERY, 2);
        DeliveryManager companyManager = DeliveryManager.create(
                UUID.randomUUID(), DEST_HUB_ID, ManagerType.COMPANY_DELIVERY, 1);

        deliveryManagerRepository.saveAllAndFlush(List.of(hubManager1, hubManager2, companyManager));
    }

    private void stubExternalServiceResponses() {
        when(userQueryPort.findUser(RECEIVER_ID)).thenReturn(Optional.of(new UserInfo(
                RECEIVER_ID, "홍길동", "COMPANY_MANAGER", "APPROVED",
                "U-RECIPIENT", RECEIVER_COMPANY_ID, "COMPANY")));
        when(companyQueryPort.findHubInfo(RECEIVER_COMPANY_ID))
                .thenReturn(Optional.of(new CompanyHubInfo(RECEIVER_COMPANY_ID, DEST_HUB_ID)));
        when(hubRouteQueryPort.findPath(
                org.mockito.ArgumentMatchers.eq(ORIGIN_HUB_ID),
                org.mockito.ArgumentMatchers.eq(DEST_HUB_ID),
                anyList()))
                .thenReturn(Optional.of(new HubRoutePathInfo(
                        ORIGIN_HUB_ID,
                        DEST_HUB_ID,
                        List.of(
                                new HubRoutePathInfo.RouteInfo(
                                        UUID.randomUUID(), 1, ORIGIN_HUB_ID, MIDDLE_HUB_ID,
                                        100_000L, 5_000L),
                                new HubRoutePathInfo.RouteInfo(
                                        UUID.randomUUID(), 2, MIDDLE_HUB_ID, DEST_HUB_ID,
                                        120_000L, 6_000L)
                        ))));
    }

    private OrderCreatedEvent createOrderCreatedEvent() {
        OrderCreatedData data = new OrderCreatedData(
                ORDER_ID,
                RECEIVER_ID,
                ORIGIN_HUB_ID,
                RECEIVER_COMPANY_ID,
                "서울특별시 중구 세종대로 1",
                LocalDateTime.of(2026, 8, 14, 18, 0),
                "도착 전 연락",
                List.of(new OrderCreatedItem(SUPPLIER_COMPANY_ID, PRODUCT_ID, 4)));

        return new OrderCreatedEvent(EVENT_ID, LocalDateTime.of(2026, 8, 12, 10, 0), data);
    }
}
