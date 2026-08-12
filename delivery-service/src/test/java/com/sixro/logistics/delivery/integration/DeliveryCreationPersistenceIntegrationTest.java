package com.sixro.logistics.delivery.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import com.sixro.logistics.delivery.application.model.DeliveryCreationData;
import com.sixro.logistics.delivery.application.service.DeliveryCreationTransactionService;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxEventType;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxStatus;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryManagerRepository;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRepository;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRouteRepository;
import com.sixro.logistics.delivery.infrastructure.repository.OutboxRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
@ImportTestcontainers(PostgresTestContainerConfig.class)
@Transactional
class DeliveryCreationPersistenceIntegrationTest {

    private static final String TRACE_ID = "delivery-creation-integration-trace-id";
    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORIGIN_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID MIDDLE_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DEST_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID SUPPLIER_COMPANY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID RECIPIENT_COMPANY_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Autowired
    private DeliveryCreationTransactionService transactionService;

    @Autowired
    private DeliveryManagerRepository deliveryManagerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private DeliveryRouteRepository deliveryRouteRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("실제 PostgreSQL에 배송과 경로를 저장하고 자동 배정 및 PENDING Outbox를 생성한다")
    void create_persistsDeliveryRoutesAssignmentsAndPendingOutbox() throws Exception {
        // given
        DeliveryManager hubManager1 = createManager(ManagerType.HUB_DELIVERY, null, 1);
        DeliveryManager hubManager2 = createManager(ManagerType.HUB_DELIVERY, null, 2);
        DeliveryManager companyManager1 = createManager(ManagerType.COMPANY_DELIVERY, DEST_HUB_ID, 1);
        deliveryManagerRepository.saveAllAndFlush(List.of(hubManager1, hubManager2, companyManager1));

        DeliveryCreationData data = createData();

        // when
        boolean created = transactionService.create(data);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(created).isTrue();

        // PostgreSQL의 delivery_schema에 생성된 실제 테이블 사용 확인
        Integer deliveryTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = 'delivery_schema' and table_name = 'p_delivery'",
                Integer.class);
        assertThat(deliveryTableCount).isEqualTo(1);

        // 배송 저장 및 목적지 허브 소속 업체 배송 담당자 자동 배정 확인
        Delivery delivery = deliveryRepository.findByOrderId(ORDER_ID).orElseThrow();
        assertThat(delivery.getDeliveryId()).isNotNull();
        assertThat(delivery.getSupplierCompanyIds()).containsExactly(SUPPLIER_COMPANY_ID);
        assertThat(delivery.getRecipientCompanyId()).isEqualTo(RECIPIENT_COMPANY_ID);
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.HUB_WAITING);
        assertThat(delivery.getDeliveryManager().getDeliveryManagerId())
                .isEqualTo(companyManager1.getDeliveryManagerId());

        // 배송경로 관계와 순번 및 허브 배송 담당자 순차 배정 확인
        List<DeliveryRoute> routes = deliveryRouteRepository
                .findAllByDelivery_DeliveryIdOrderByRouteSequenceAsc(delivery.getDeliveryId());
        assertThat(routes).hasSize(2);
        assertThat(routes).extracting(DeliveryRoute::getRouteSequence).containsExactly(1, 2);
        assertThat(routes).extracting(DeliveryRoute::getRouteStatus)
                .containsOnly(RouteStatus.HUB_TRANSIT_WAITING);
        assertThat(routes).extracting(route -> route.getDelivery().getDeliveryId())
                .containsOnly(delivery.getDeliveryId());
        assertThat(routes).extracting(route -> route.getDeliveryManager().getDeliverySequence())
                .containsExactly(1, 2);

        // 배송 생성 이벤트가 Kafka 발행 전 PENDING Outbox로 저장되었는지 확인
        List<Outbox> outboxes = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        assertThat(outboxes).hasSize(1);
        Outbox outbox = outboxes.getFirst();
        assertThat(outbox.getAggregateId()).isEqualTo(delivery.getDeliveryId());
        assertThat(outbox.getEventType()).isEqualTo(OutboxEventType.DELIVERY_CREATED);
        assertThat(outbox.getTraceId()).isEqualTo(TRACE_ID);
        assertThat(outbox.getPublishedAt()).isNull();

        JsonNode payload = objectMapper.readTree(outbox.getPayload());
        assertThat(payload.path("data").path("orderId").asText()).isEqualTo(ORDER_ID.toString());
        assertThat(payload.path("data").path("deliveryId").asText())
                .isEqualTo(delivery.getDeliveryId().toString());
        assertThat(payload.path("data").path("routes")).hasSize(2);
        assertThat(payload.path("data").path("totalHubRouteExpectedDurationS").asLong())
                .isEqualTo(11_000L);
    }

    private DeliveryCreationData createData() {
        return new DeliveryCreationData(
                TRACE_ID,
                ORDER_ID,
                List.of(new DeliveryCreationData.ProductData(PRODUCT_ID, 4)),
                Set.of(SUPPLIER_COMPANY_ID),
                RECIPIENT_COMPANY_ID,
                ORIGIN_HUB_ID,
                DEST_HUB_ID,
                "서울특별시 중구 세종대로 1",
                LocalDateTime.of(2026, 8, 14, 18, 0),
                "도착 전 연락",
                "홍길동",
                "U-RECIPIENT",
                List.of(
                        new DeliveryCreationData.RouteData(
                                1, ORIGIN_HUB_ID, MIDDLE_HUB_ID, 100_000L, 5_000L),
                        new DeliveryCreationData.RouteData(
                                2, MIDDLE_HUB_ID, DEST_HUB_ID, 120_000L, 6_000L)
                )
        );
    }

    private DeliveryManager createManager(ManagerType managerType, UUID hubId, int sequence) {
        return DeliveryManager.create(UUID.randomUUID(), hubId, managerType, sequence);
    }
}
