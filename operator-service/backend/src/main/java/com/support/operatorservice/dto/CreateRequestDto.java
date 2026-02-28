package com.support.operatorservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateRequestDto {
    private String id;
    private String email;
    private String organization;
    private String fio;
    private String phone;
    @JsonProperty("device_type")
    private String deviceType;
    @JsonProperty("serial_number")
    private String serialNumber;
    private String category;
    private String project;
    private String inn;
    @JsonProperty("country_region")
    private String countryRegion;
    private byte[] file;
    @JsonProperty("confidence_score")
    private Float confidenceScore;
    private String status;
    @JsonProperty("ai_generated_answer")
    private String aiGeneratedAnswer;
    @JsonProperty("operator_answer")
    private String operatorAnswer;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    @JsonProperty("is_form")
    private Boolean isForm;
    @JsonProperty("user_message")
    private String userMessage;
}
