package com.mailserver.mailprocessor.service;

import com.mailserver.mailprocessor.model.dto.RequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerExample {

    @KafkaListener(
        topics = "${app.kafka.topic.requests}",
        groupId = "ai-agent-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRequest(RequestDto request) {
        log.info("=== Received Request from Kafka ===");
        log.info("ID: {}", request.getId());
        log.info("Email: {}", request.getEmail());
        log.info("Organization: {}", request.getOrganization());
        log.info("FIO: {}", request.getFio());
        log.info("Phone: {}", request.getPhone());
        log.info("Device Type: {}", request.getDeviceType());
        log.info("Serial Number: {}", request.getSerialNumber());
        log.info("Category: {}", request.getCategory());
        log.info("Status: {}", request.getStatus());
        log.info("Subject: {}", request.getEmailSubject());
        log.info("===================================");
    }
}
