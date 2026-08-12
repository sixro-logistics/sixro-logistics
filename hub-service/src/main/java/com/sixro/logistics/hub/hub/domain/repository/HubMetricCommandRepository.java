package com.sixro.logistics.hub.hub.domain.repository;

import com.sixro.logistics.hub.hub.domain.model.HubMetric;

import java.util.List;

public interface HubMetricCommandRepository {
    void saveAll(List<HubMetric> metrics);
}
