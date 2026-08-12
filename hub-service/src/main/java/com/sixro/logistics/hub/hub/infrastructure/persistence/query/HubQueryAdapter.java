package com.sixro.logistics.hub.hub.infrastructure.persistence.query;

import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubStatus;
import com.sixro.logistics.hub.hub.domain.model.HubWithDistance;
import com.sixro.logistics.hub.hub.domain.model.HubZone;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HubQueryAdapter implements com.sixro.logistics.hub.hub.domain.repository.HubQueryRepository {

    private final HubQueryRepository queryRepository;

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

    @Override
    public List<Hub> findByIdIn(Set<UUID> ids) {
        return queryRepository.findByIdIn(ids);
    }

    @Override
    public List<Hub> findAllOperatingHubs() {
        // CLOSED 상태가 아닌(ACTIVE, CONGESTED, MAINTENANCE) 허브만 필터링하여 반환합니다.
        return queryRepository.findAllByHubStatusNot(HubStatus.CLOSED);
    }
}