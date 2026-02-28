package com.mailserver.mailprocessor.service;

import com.mailserver.mailprocessor.model.dto.RequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerService {

    private final KafkaTemplate<String, RequestDto> kafkaTemplate;

    @Value("${app.kafka.topic.requests}")
    private String requestsTopic;

    public KafkaProducerService(KafkaTemplate<String, RequestDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send request to Kafka topic with timeout
     */
    public void sendRequest(RequestDto request) {
        try {
            String key = request.getId().toString();
            
            CompletableFuture<SendResult<String, RequestDto>> future = 
                kafkaTemplate.send(requestsTopic, key, request);
            
            // Wait for result with timeout
            SendResult<String, RequestDto> result = future.get(10, TimeUnit.SECONDS);
            
            log.info("Sent request to Kafka: id={}, topic={}, partition={}, offset={}", 
                    request.getId(),
                    requestsTopic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            
        } catch (Exception e) {
            log.error("Failed to send request to Kafka: id={}, error={}", 
                    request.getId(), e.getMessage());
            throw new RuntimeException("Kafka send failed", e);
        }
    }
}
