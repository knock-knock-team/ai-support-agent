package com.support.operatorservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false, length = 100)
    private String senderEmail;
    
    @Column(nullable = false, length = 200)
    private String email;

    @Column(length = 255)
    private String organization;

    @Column(length = 255)
    private String fio;

    @Column(length = 50)
    private String phone;

    @Column(name = "device_type", length = 255)
    private String deviceType;

    @Column(name = "serial_number", length = 255)
    private String serialNumber;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(name = "category", length = 50)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(length = 255)
    private String project;

    @Column(length = 20)
    private String inn;

    @Column(name = "country_region", length = 255)
    private String countryRegion;

    @Column(name = "file_data", columnDefinition = "bytea")
    private byte[] file;

    @Column(name = "confidence_score", nullable = false)
    private Float confidenceScore;

    @Column(name = "ai_generated_answer", columnDefinition = "TEXT")
    private String aiGeneratedAnswer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;
    
    @Column(name = "operator_answer", columnDefinition = "TEXT")
    private String operatorAnswer;
    
    @Column(columnDefinition = "TEXT")
    private String operatorNotes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Status {
        NEW,
        AI_GENERATED,
        OPERATOR_REVIEW,
        CLOSED
    }
    
    public enum Category {
        TECHNICAL,
        BILLING,
        ACCOUNT,
        GENERAL,
        OTHER
    }
}
