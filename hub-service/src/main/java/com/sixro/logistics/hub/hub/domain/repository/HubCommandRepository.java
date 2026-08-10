package com.sixro.logistics.hub.hub.domain.repository;

import com.sixro.logistics.hub.hub.domain.model.Hub;
import java.util.Optional;
import java.util.UUID;

public interface HubCommandRepository {
    Hub save(Hub hub);
    Optional<Hub> findById(UUID id);
    boolean existsByHubName(String hubName);
}