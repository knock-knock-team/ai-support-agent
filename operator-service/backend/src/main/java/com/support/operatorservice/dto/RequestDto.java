package com.support.operatorservice.dto;

import com.support.operatorservice.entity.Request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDto {
    private Long id;
    private String subject;
    private String userMessage;
    private String aiResponse;
    private Double confidence;
    private String status;
    private String category;
    private String senderEmail;
    private String operatorResponse;
    private String operatorNotes;
    private UserDto operator;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    
    public static RequestDto fromEntity(Request request) {
        return RequestDto.builder()
                .id(request.getId())
                .subject(request.getSubject())
                .userMessage(request.getUserMessage())
                .aiResponse(request.getAiResponse())
                .confidence(request.getConfidence())
                .status(request.getStatus().name())
                .category(request.getCategory() != null ? request.getCategory().name() : null)
                .senderEmail(request.getSenderEmail())
                .operatorResponse(request.getOperatorResponse())
                .operatorNotes(request.getOperatorNotes())
                .operator(request.getOperator() != null ? UserDto.fromEntity(request.getOperator()) : null)
                .createdAt(request.getCreatedAt())
                .respondedAt(request.getRespondedAt())
                .build();
    }
}
