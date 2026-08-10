package com.sixro.logistics.hub.integration;

import com.sixro.logistics.common.test.config.KafkaTestContainerConfig;
import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers({
        PostgresTestContainerConfig.class,
        KafkaTestContainerConfig.class
})
class KafkaIntegrationTest {

    private static final String TEST_TOPIC = "isolated-kafka-test-topic";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TestKafkaConsumer testKafkaConsumer;

    @Test
    @DisplayName("Kafka 컨테이너에 메시지를 발행하고 소비할 수 있어야 한다")
    void shouldProduceAndConsumeMessage() throws InterruptedException {
        // given
        String messageKey = "key-1";
        String messagePayload = "Hello Testcontainers Kafka!";

        // when
        kafkaTemplate.send(TEST_TOPIC, messageKey, messagePayload);

        // then
        String receivedMessage = testKafkaConsumer.getLatestMessage(10, TimeUnit.SECONDS);

        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage).isEqualTo(messagePayload);
    }

    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        public TestKafkaConsumer testKafkaConsumer() {
            return new TestKafkaConsumer();
        }
    }

    static class TestKafkaConsumer {
        private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        @KafkaListener(topics = TEST_TOPIC, groupId = "isolated-kafka-test-group")
        public void listen(String message) {
            messageQueue.add(message);
        }

        public String getLatestMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return messageQueue.poll(timeout, unit);
        }
    }
}