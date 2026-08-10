package com.sixro.logistics.hub.hub.infrastructure.persistence.query;

import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubWithDistance;
import com.sixro.logistics.hub.hub.domain.model.HubZone;
import com.sixro.logistics.hub.hub.domain.repository.HubQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HubQueryAdapter implements HubQueryRepository {

    private final HubNativeQueryRepository queryRepository;

    @Override
    public Optional<Hub> findById(UUID id) {
        return queryRepository.findById(id);
    }

    @Override
    public Page<Hub> search(HubZone hubZone, String hubName, Pageable pageable) {
        return queryRepository.searchHubs(hubZone, hubName, pageable);
    }

    @Override
    public Optional<HubWithDistance> findNearestHubWithDistance(double longitude, double latitude) {
        return queryRepository.findNearestHubWithDistance(longitude, latitude)
                .map(projection -> new HubWithDistance(
                        projection.getHubId(),
                        projection.getHubName(),
                        projection.getHubStatus(),
                        projection.getDistanceInMeters()
                ));
    }
}