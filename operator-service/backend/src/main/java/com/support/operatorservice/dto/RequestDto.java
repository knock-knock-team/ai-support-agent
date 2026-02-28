package com.support.operatorservice.dto;

import com.support.operatorservice.entity.Request;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDto {
    private Long id;
    private String email;
    private String organization;
    private String fio;
    private String phone;
    @JsonProperty("device_type")
    private String deviceType;
    @JsonProperty("serial_number")
    private String serialNumber;
    private String status;
    private String category;
    private String project;
    private String inn;
    @JsonProperty("country_region")
    private String countryRegion;
    @JsonProperty("confidence_score")
    private Float confidenceScore;
    @JsonProperty("ai_generated_answer")
    private String aiGeneratedAnswer;
    @JsonProperty("operator_answer")
    private String operatorAnswer;
    private String operatorNotes;
    private UserDto operator;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    @JsonProperty("responded_at")
    private OffsetDateTime respondedAt;
    @JsonProperty("is_form")
    private Boolean isForm;
    @JsonProperty("user_message")
    private String userMessage;
    
    public static RequestDto fromEntity(Request request) {
        return RequestDto.builder()
                .id(request.getId())
            .email(request.getEmail())
            .organization(request.getOrganization())
            .fio(request.getFio())
            .phone(request.getPhone())
            .deviceType(request.getDeviceType())
            .serialNumber(request.getSerialNumber())
            .status(request.getStatus().name().toLowerCase())
                .category(request.getCategory() != null ? request.getCategory().name() : null)
            .project(request.getProject())
            .inn(request.getInn())
            .countryRegion(request.getCountryRegion())
            .confidenceScore(request.getConfidenceScore())
            .aiGeneratedAnswer(request.getAiGeneratedAnswer())
            .operatorAnswer(request.getOperatorAnswer())
                .operatorNotes(request.getOperatorNotes())
                .operator(request.getOperator() != null ? UserDto.fromEntity(request.getOperator()) : null)
                .createdAt(request.getCreatedAt())
            .updatedAt(request.getUpdatedAt())
                .respondedAt(request.getRespondedAt())
                .isForm(request.getIsForm())
                .userMessage(request.getUserMessage())
                .build();
    }
}
