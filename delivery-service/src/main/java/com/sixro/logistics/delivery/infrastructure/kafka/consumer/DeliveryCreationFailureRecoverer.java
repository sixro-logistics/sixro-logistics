package com.sixro.logistics.delivery.infrastructure.kafka.consumer;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.delivery.application.service.OutboxService;
import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;
import com.sixro.logistics.delivery.application.event.DeliveryCreationFailedEvent;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryCreationFailureRecoverer implements ConsumerRecordRecoverer {

    private final OutboxService outboxService;
    private final Tracer tracer;

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        // 원본 주문 이벤트 확인
        if (!(record.value() instanceof OrderCreatedEvent orderCreatedEvent)
                || orderCreatedEvent.eventId() == null || orderCreatedEvent.data() == null
                || orderCreatedEvent.data().orderId() == null) {
            throw new IllegalArgumentException("배송 생성 실패 이벤트에 필요한 주문 정보를 확인할 수 없습니다.", exception);
        }

        // 배송 생성 실패 유형 확인
        DeliveryCreationKafkaErrorCode errorCode = resolveErrorCode(exception);
        String traceId = resolveCurrentTraceId(orderCreatedEvent.eventId());

        // 배송 생성 실패 이벤트 구성
        DeliveryCreationFailedEvent.DeliveryCreationFailedData eventData =
                new DeliveryCreationFailedEvent.DeliveryCreationFailedData(
                        orderCreatedEvent.eventId(), orderCreatedEvent.data().orderId(),
                        errorCode, errorCode.getMessage()
                );
        DeliveryCreationFailedEvent event = new DeliveryCreationFailedEvent(
                UUID.randomUUID(), LocalDateTime.now(), eventData
        );

        // 실패 Outbox 이벤트 저장
        outboxService.save(event, traceId);

        log.error("OrderCreatedEvent 배송 생성 최종 실패: eventId={}, orderId={}, failureCode={}, traceId={}",
                orderCreatedEvent.eventId(), orderCreatedEvent.data().orderId(), errorCode.name(), traceId, exception);
    }

    private DeliveryCreationKafkaErrorCode resolveErrorCode(Exception exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof BaseException baseException
                    && baseException.getErrorCode() instanceof DeliveryCreationKafkaErrorCode errorCode) {
                return errorCode;
            }
            cause = cause.getCause();
        }

        return DeliveryCreationKafkaErrorCode.DELIVERY_CREATION_FAILED;
    }

    private String resolveCurrentTraceId(UUID orderCreatedEventId) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return orderCreatedEventId.toString();
        }
        return currentSpan.context().traceId();
    }
}
