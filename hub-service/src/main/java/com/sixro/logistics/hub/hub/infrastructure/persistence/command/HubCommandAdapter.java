package com.sixro.logistics.hub.hub.infrastructure.persistence.command;

import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.repository.HubCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HubCommandAdapter implements HubCommandRepository {

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
}