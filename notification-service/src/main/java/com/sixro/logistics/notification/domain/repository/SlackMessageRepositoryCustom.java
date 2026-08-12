package com.sixro.logistics.notification.domain.repository;


import com.sixro.logistics.notification.domain.entity.SlackMessage;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SlackMessageRepositoryCustom {
    Page<SlackMessage> searchMessages(SlackMessageSearchCondition condition, Pageable pageable);
}
