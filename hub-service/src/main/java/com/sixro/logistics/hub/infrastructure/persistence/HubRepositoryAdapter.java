package com.sixro.logistics.hub.infrastructure.persistence;

import com.sixro.logistics.hub.domain.model.Hub;
import com.sixro.logistics.hub.domain.model.HubWithDistance;
import com.sixro.logistics.hub.domain.model.HubZone;
import com.sixro.logistics.hub.domain.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HubRepositoryAdapter implements HubRepository {

    private final HubJpaRepository hubJpaRepository;

    @Override
    public Hub save(Hub hub) {
        return hubJpaRepository.save(hub);
    }

    @Override
    public Optional<Hub> findById(UUID id) {
        return hubJpaRepository.findById(id);
    }

    @Override
    public boolean existsByHubName(String hubName) {
        return hubJpaRepository.existsByHubName(hubName);
    }

    @Override
    public Page<Hub> search(HubZone hubZone, String hubName, Pageable pageable) {
        return hubJpaRepository.searchHubs(hubZone, hubName, pageable);
    }

    @Override
    public Optional<HubWithDistance> findNearestHubWithDistance(double longitude, double latitude) {
        return hubJpaRepository.findNearestHubWithDistance(longitude, latitude)
                .map(projection -> new HubWithDistance(
                        projection.getHubId(),
                        projection.getHubName(),
                        projection.getHubStatus(),
                        projection.getDistanceInMeters()
                ));
    }
}