package com.sixro.logistics.hub.hub.infrastructure.persistence.command;

import com.sixro.logistics.hub.hub.domain.model.HubMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HubMetricJpaRepository extends JpaRepository<HubMetric, UUID> {
    List<HubMetric> findAllByHubIdIn(List<UUID> hubIds);
}