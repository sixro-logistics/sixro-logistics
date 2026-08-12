package com.sixro.logistics.hub.hub.domain.repository;

import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubWithDistance;
import com.sixro.logistics.hub.hub.domain.model.HubZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HubQueryRepository {
    Optional<Hub> findById(UUID id);
    Page<Hub> search(HubZone hubZone, String hubName, Pageable pageable);
    Optional<HubWithDistance> findNearestHubWithDistance(double longitude, double latitude);
    List<Hub> findByIdIn(Set<UUID> ids);
    List<Hub> findAllOperatingHubs();
}