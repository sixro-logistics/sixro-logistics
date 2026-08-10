package com.sixro.logistics.hub.hub.infrastructure.persistence.command;

import com.sixro.logistics.hub.hub.domain.model.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HubJpaRepository extends JpaRepository<Hub, UUID> {
    boolean existsByHubName(String hubName);
}