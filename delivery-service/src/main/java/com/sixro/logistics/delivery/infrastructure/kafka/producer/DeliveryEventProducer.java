package com.sixro.logistics.delivery.infrastructure.kafka.producer;

import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
import com.sixro.logistics.delivery.infrastructure.kafka.event.DeliveryCreatedEvent;
import com.sixro.logistics.delivery.infrastructure.kafka.event.DeliveryCreationFailedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

@Component
public class DeliveryEventProducer {

    private static final String EVENT_TYPE_HEADER = "event-type";
    private static final String TRACE_ID_HEADER = "trace-id";
    private static final String DELIVERY_CREATED_EVENT_TYPE = "DeliveryCreatedEvent";
    private static final String DELIVERY_CREATION_FAILED_EVENT_TYPE = "DeliveryCreationFailedEvent";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DeliveryEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendDeliveryCreatedEvent(DeliveryCreatedEvent event, String traceId)
            throws ExecutionException, InterruptedException {

        // 배송 ID를 Partition Key로 설정
        String partitionKey = event.data().deliveryId().toString();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaTopics.DELIVERY_CREATED, partitionKey, event
        );

        // Header 구성
        RecordHeader eventTypeHeader = new RecordHeader(
                EVENT_TYPE_HEADER, DELIVERY_CREATED_EVENT_TYPE.getBytes(StandardCharsets.UTF_8)
        );
        RecordHeader traceIdHeader = new RecordHeader(
                TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8)
        );

        record.headers().add(eventTypeHeader);
        record.headers().add(traceIdHeader);

        // 이벤트 발행 결과 확인
        kafkaTemplate.send(record).get();
    }

    public void sendDeliveryCreationFailedEvent(DeliveryCreationFailedEvent event, String traceId)
            throws ExecutionException, InterruptedException {

        // 주문 ID를 Partition Key로 설정
        String partitionKey = event.data().orderId().toString();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaTopics.DELIVERY_CREATION_FAILED, partitionKey, event
        );

        // Header 구성
        RecordHeader eventTypeHeader = new RecordHeader(
                EVENT_TYPE_HEADER, DELIVERY_CREATION_FAILED_EVENT_TYPE.getBytes(StandardCharsets.UTF_8)
        );
        RecordHeader traceIdHeader = new RecordHeader(
                TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8)
        );

        record.headers().add(eventTypeHeader);
        record.headers().add(traceIdHeader);

        // 이벤트 발행 결과 확인
        kafkaTemplate.send(record).get();
    }
}
