package com.sixro.logistics.hub.domain.repository;

import com.sixro.logistics.hub.domain.model.Hub;
import com.sixro.logistics.hub.domain.model.HubWithDistance;
import com.sixro.logistics.hub.domain.model.HubZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface HubRepository {
    Hub save(Hub hub);
    Optional<Hub> findById(UUID id);
    boolean existsByHubName(String hubName);
    Page<Hub> search(HubZone hubZone, String hubName, Pageable pageable);
    Optional<HubWithDistance> findNearestHubWithDistance(double longitude, double latitude);
}
