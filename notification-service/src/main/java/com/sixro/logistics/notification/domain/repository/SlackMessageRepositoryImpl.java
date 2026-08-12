package com.sixro.logistics.notification.domain.repository;

import com.sixro.logistics.notification.domain.entity.QSlackMessage;
import com.sixro.logistics.notification.domain.entity.SlackMessage;
import com.sixro.logistics.notification.presentation.dto.request.SlackMessageSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class SlackMessageRepositoryImpl implements SlackMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 클래스 내부에 QSlackMessage 인스턴스를 직접 선언
    private final QSlackMessage slackMessage = QSlackMessage.slackMessage;

    @Override
    public Page<SlackMessage> searchMessages(SlackMessageSearchCondition condition, Pageable pageable) {
        List<SlackMessage> content = queryFactory
                .selectFrom(slackMessage)
                .where(
                        slackMessage.isDeleted.eq(false),
                        eqSenderType(condition),
                        eqSenderId(condition),
                        eqMessageType(condition),
                        eqSendStatus(condition)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(slackMessage.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(slackMessage.count())
                .from(slackMessage)
                .where(
                        slackMessage.isDeleted.eq(false),
                        eqSenderType(condition),
                        eqSenderId(condition),
                        eqMessageType(condition),
                        eqSendStatus(condition)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    private BooleanExpression eqSenderType(SlackMessageSearchCondition condition) {
        return condition.getSenderType() != null ? slackMessage.senderType.eq(condition.getSenderType()) : null;
    }

    private BooleanExpression eqSenderId(SlackMessageSearchCondition condition) {
        return condition.getSenderId() != null ? slackMessage.senderId.eq(condition.getSenderId()) : null;
    }

    private BooleanExpression eqMessageType(SlackMessageSearchCondition condition) {
        return condition.getMessageType() != null ? slackMessage.messageType.eq(condition.getMessageType()) : null;
    }

    private BooleanExpression eqSendStatus(SlackMessageSearchCondition condition) {
        return condition.getSendStatus() != null ? slackMessage.sendStatus.eq(condition.getSendStatus()) : null;
    }
}
