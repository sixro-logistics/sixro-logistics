package com.sixro.logistics.hub.hub.infrastructure.persistence.command;

import com.sixro.logistics.hub.hub.domain.model.HubMetric;
import com.sixro.logistics.hub.hub.domain.repository.HubMetricCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HubMetricCommandAdapter implements HubMetricCommandRepository {

    private final HubMetricJpaRepository jpaRepository;

    @Override
    public void saveAll(List<HubMetric> metrics) {
        jpaRepository.saveAll(metrics);
    }
}