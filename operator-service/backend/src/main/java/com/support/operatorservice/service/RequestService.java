package com.support.operatorservice.service;

import com.support.operatorservice.entity.Request;
import com.support.operatorservice.entity.User;
import com.support.operatorservice.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RequestService {
    
    private final RequestRepository requestRepository;
    
    public Request findById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }
    
    public List<Request> findAll() {
        return requestRepository.findAll();
    }
    
    public List<Request> findPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(Request.Status.PENDING);
    }
    
    public List<Request> findByOperator(Long operatorId) {
        return requestRepository.findByOperatorId(operatorId);
    }
    
    @Transactional
    public Request createRequest(String subject, String userMessage, String aiResponse, 
                                 Double confidence, Request.Category category, String senderEmail) {
        Request request = Request.builder()
                .subject(subject)
                .userMessage(userMessage)
                .aiResponse(aiResponse)
                .confidence(confidence)
                .status(confidence < 0.7 ? Request.Status.PENDING : Request.Status.APPROVED)
                .category(category)
                .senderEmail(senderEmail)
                .build();
        
        return requestRepository.save(request);
    }
    
    @Transactional
    public Request approveAiResponse(Long requestId, User operator) {
        Request request = findById(requestId);
        request.setStatus(Request.Status.APPROVED);
        request.setOperator(operator);
        request.setOperatorResponse(request.getAiResponse());
        return requestRepository.save(request);
    }
    
    @Transactional
    public Request updateOperatorResponse(Long requestId, String operatorResponse, 
                                         String notes, User operator) {
        Request request = findById(requestId);
        request.setOperatorResponse(operatorResponse);
        request.setOperatorNotes(notes);
        request.setOperator(operator);
        request.setStatus(Request.Status.IN_REVIEW);
        return requestRepository.save(request);
    }
    
    @Transactional
    public Request sendResponse(Long requestId) {
        Request request = findById(requestId);
        request.setStatus(Request.Status.SENT);
        request.setRespondedAt(LocalDateTime.now());
        return requestRepository.save(request);
    }
    
    public Map<String, Long> getStatsByStatus() {
        Map<String, Long> stats = new HashMap<>();
        for (Request.Status status : Request.Status.values()) {
            stats.put(status.name(), requestRepository.countByStatus(status));
        }
        return stats;
    }
    
    public Map<String, Long> getStatsByCategory() {
        Map<String, Long> stats = new HashMap<>();
        for (Request.Category category : Request.Category.values()) {
            stats.put(category.name(), requestRepository.countByCategory(category));
        }
        return stats;
    }
    
    public List<Request> getRecentRequests(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return requestRepository.findRecentRequests(startDate);
    }
}
