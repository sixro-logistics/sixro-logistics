package com.sixro.logistics.hub.hub.domain.repository;

import com.sixro.logistics.hub.hub.domain.model.HubMetric;

import java.util.List;
import java.util.UUID;

public interface HubMetricQueryRepository {
    List<HubMetric> findAllByHubIdIn(List<UUID> hubIds);
}