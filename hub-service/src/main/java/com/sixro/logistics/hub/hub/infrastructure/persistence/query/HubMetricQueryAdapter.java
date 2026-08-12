package com.sixro.logistics.hub.hub.infrastructure.persistence.query;

import com.sixro.logistics.hub.hub.domain.model.HubMetric;
import com.sixro.logistics.hub.hub.domain.repository.HubMetricQueryRepository;
import com.sixro.logistics.hub.hub.infrastructure.persistence.command.HubMetricJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HubMetricQueryAdapter implements HubMetricQueryRepository {

    private final HubMetricJpaRepository jpaRepository;

    @Override
    public List<HubMetric> findAllByHubIdIn(List<UUID> hubIds) {
        return jpaRepository.findAllByHubIdIn(hubIds);
    }
}
