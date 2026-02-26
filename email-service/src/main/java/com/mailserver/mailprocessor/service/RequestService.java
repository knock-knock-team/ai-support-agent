package com.mailserver.mailprocessor.service;

import com.mailserver.mailprocessor.mapper.RequestMapper;
import com.mailserver.mailprocessor.model.dto.RequestDto;
import com.mailserver.mailprocessor.model.entity.Request;
import com.mailserver.mailprocessor.repository.RequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class RequestService {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final KafkaProducerService kafkaProducerService;

    public RequestService(RequestRepository requestRepository,
                         RequestMapper requestMapper,
                         ObjectProvider<KafkaProducerService> kafkaProducerServiceProvider) {
        this.requestRepository = requestRepository;
        this.requestMapper = requestMapper;
        this.kafkaProducerService = kafkaProducerServiceProvider.getIfAvailable();
    }

    /**
     * Process and save request, then send to Kafka
     * IMPORTANT: Request is saved to DB first to prevent data loss
     */
    @Transactional
    public RequestDto processRequest(RequestDto requestDto) {
        try {
            log.debug("Processing request from email: {}", requestDto.getEmail());
            
            // Step 1: Convert to entity and save to database (persistence layer)
            Request request = requestMapper.toEntity(requestDto);
            Request savedRequest = requestRepository.save(request);
            
            // Convert back to DTO
            RequestDto savedDto = requestMapper.toDto(savedRequest);
            
                log.info("Saved request to database: id={}, email={}", 
                    savedRequest.getId(), savedRequest.getEmail());
            
            // Step 2: Send to Kafka for AI processing (non-blocking, optional)
            if (kafkaProducerService != null) {
                try {
                    kafkaProducerService.sendRequest(savedDto);
                } catch (Exception kafkaError) {
                    // Request is already saved in DB, so it won't be lost
                        log.warn("Failed to send request to Kafka, but it's saved in DB: id={}, error={}", 
                            savedDto.getId(), kafkaError.getMessage());
                    // Note: В будущем здесь можно добавить retry механизм или dead letter queue
                }
            } else {
                log.debug("Kafka is disabled, skipping Kafka send");
            }
            
            return savedDto;
            
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process request", e);
        }
    }

    /**
     * Process multiple requests
     */
    @Transactional
    public List<RequestDto> processRequests(List<RequestDto> requests) {
        return requests.stream()
                .map(this::processRequest)
                .toList();
    }
}
