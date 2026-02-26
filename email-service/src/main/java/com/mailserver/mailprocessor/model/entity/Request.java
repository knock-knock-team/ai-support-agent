package com.mailserver.mailprocessor.model.entity;

import com.mailserver.mailprocessor.model.enums.RequestCategory;
import com.mailserver.mailprocessor.model.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String email;
    
    private String organization;
    
    private String fio;
    
    private String phone;
    
    @Column(name = "device_type")
    private String deviceType;
    
    @Column(name = "serial_number")
    private String serialNumber;
    
    @Enumerated(EnumType.STRING)
    private RequestCategory category;
    
    private String project;
    
    private String inn;
    
    @Column(name = "country_region")
    private String countryRegion;
    
    @Lob
    private byte[] file;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_content_type")
    private String fileContentType;
    
    @Column(name = "confidence_score")
    private Float confidenceScore;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.NEW;
    
    @Column(columnDefinition = "TEXT")
    private String aiGeneratedAnswer;
    
    @Column(columnDefinition = "TEXT")
    private String operatorAnswer;
    
    @Column(name = "email_subject")
    private String emailSubject;
    
    @Column(columnDefinition = "TEXT")
    private String emailBody;
    
    @Column(name = "raw_email_content", columnDefinition = "TEXT")
    private String rawEmailContent;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
