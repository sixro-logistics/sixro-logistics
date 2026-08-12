package com.sixro.logistics.notification.domain.repository;

import com.sixro.logistics.notification.domain.entity.AIRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiRequestLogRepository extends JpaRepository<AIRequestLog, UUID> {
}
