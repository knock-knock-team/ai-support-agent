package com.mailserver.mailprocessor.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mailserver.mailprocessor.model.enums.RequestCategory;
import com.mailserver.mailprocessor.model.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestDto {
    
    private UUID id;
    
    private String email;
    
    private String organization;
    
    private String fio;
    
    private String phone;
    
    private String deviceType;
    
    private String serialNumber;
    
    private RequestCategory category;
    
    private String project;
    
    private String inn;
    
    private String countryRegion;
    
    private byte[] file;
    
    private String fileName;
    
    private String fileContentType;
    
    private Float confidenceScore;
    
    @Builder.Default
    private RequestStatus status = RequestStatus.NEW;
    
    private String aiGeneratedAnswer;
    
    private String operatorAnswer;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Email metadata
    private String emailSubject;
    private String emailBody;
    private String rawEmailContent;
}
