package com.sixro.logistics.hub.integration;

import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import com.sixro.logistics.hub.config.TestAuditingConfig;
import com.sixro.logistics.hub.hub.domain.model.Address;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubZone;
import com.sixro.logistics.hub.hub.domain.model.Location;
import com.sixro.logistics.hub.hub.infrastructure.persistence.command.HubJpaRepository;
import com.sixro.logistics.hub.hub.infrastructure.persistence.query.HubNativeQueryRepository;
import com.sixro.logistics.hub.hub.infrastructure.persistence.query.NearestHubProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers({
        PostgresTestContainerConfig.class
})
@Import(TestAuditingConfig.class)
@Transactional
class HubRepositoryIntegrationTest {

    @Autowired
    private HubJpaRepository hubJpaRepository;

    @Autowired
    private HubNativeQueryRepository hubNativeQueryRepository;

    @Test
    @DisplayName("Hub 정보를 저장하면 데이터베이스에 정상적으로 반영된다")
    void save_hub_test() {
        // given
        Hub hub = Hub.builder()
                .hubName("서울특별시 센터")
                .address(Address.of("05838", "서울특별시 송파구 송파대로 55", "서울특별시 송파구 장지동 862", "동남권물류단지 A동"))
                .location(Location.of(127.1249, 37.4776))
                .hubZone(HubZone.CAPITAL)
                .maxCapacity(1_000_000)
                .build();

        // when
        Hub savedHub = hubJpaRepository.save(hub);

        // then
        assertThat(savedHub.getId())
                .isNotNull();

        assertThat(savedHub.getHubName())
                .isEqualTo("서울특별시 센터");
    }

    @Test
    @DisplayName("PostGIS Point 데이터를 저장하고 조회할 때 좌표 값이 일치해야 한다")
    void save_postgis_point_test() {
        // given
        Hub hub = Hub.builder()
                .hubName("서울특별시 센터")
                .address(Address.of("05838", "서울특별시 송파구 송파대로 55", "서울특별시 송파구 장지동 862", "동남권물류단지 A동"))
                .location(Location.of(127.1249, 37.4776))
                .hubZone(HubZone.CAPITAL)
                .maxCapacity(1_000_000)
                .build();

        // when
        Hub saved = hubJpaRepository.save(hub);
        Hub result = hubJpaRepository.findById(saved.getId())
                .orElseThrow();

        // then
        assertThat(result.getLocation())
                .isNotNull();

        assertThat(result.getLocation().getLongitude())
                .isEqualTo(127.1249);

        assertThat(result.getLocation().getLatitude())
                .isEqualTo(37.4776);
    }

    @Test
    @DisplayName("특정 좌표를 기준으로 가장 가까운 허브와 거리를 조회할 수 있다")
    void find_nearest_hub_with_distance_test() {
        // given
        saveHub("서울특별시 센터", 127.1249, 37.4776);
        saveHub("부산 센터", 129.0756, 35.1796);

        // when
        Optional<NearestHubProjection> result =
                hubNativeQueryRepository.findNearestHubWithDistance(
                        127.1250,
                        37.4777
                );

        // then
        assertThat(result)
                .isPresent();

        assertThat(result.get().getHubName())
                .isEqualTo("서울특별시 센터");

        assertThat(result.get().getDistanceInMeters())
                .isNotNull();
    }

    private void saveHub(
            String name,
            double longitude,
            double latitude
    ) {
        Hub hub = Hub.builder()
                .hubName("서울특별시 센터")
                .address(Address.of("05838", "서울특별시 송파구 송파대로 55", "서울특별시 송파구 장지동 862", "동남권물류단지 A동"))
                .location(Location.of(127.1249, 37.4776))
                .hubZone(HubZone.CAPITAL)
                .maxCapacity(1_000_000)
                .build();

        hubJpaRepository.save(hub);
        hubJpaRepository.flush();
    }
}