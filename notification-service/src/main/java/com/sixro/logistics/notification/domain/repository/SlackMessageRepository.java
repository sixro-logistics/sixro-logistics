package com.sixro.logistics.notification.domain.repository;

import com.sixro.logistics.notification.domain.entity.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID>, SlackMessageRepositoryCustom {
    Optional<SlackMessage> findBySlackMessageIdAndIsDeletedFalse(UUID slackMessageId);
}
