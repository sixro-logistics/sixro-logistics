package com.sixro.logistics.hub.route.infrastructure.persistence.command;

import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.repository.HubRouteCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HubRouteCommandAdapter implements HubRouteCommandRepository {

    private final HubRouteJpaRepository jpaRepository;

    @Override
    public HubRoute save(HubRoute hubRoute) {
        return jpaRepository.save(hubRoute);
    }

    @Override
    public Optional<HubRoute> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsByOriginHubIdAndDestinationHubId(UUID originHubId, UUID destinationHubId) {
        return jpaRepository.existsByOriginHubIdAndDestinationHubId(originHubId, destinationHubId);
    }
}