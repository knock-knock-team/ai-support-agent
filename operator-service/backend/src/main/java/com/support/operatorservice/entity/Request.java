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
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private Category category;
    
    @Column(nullable = false, length = 100)
    private String senderEmail;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;
    
    @Column(columnDefinition = "TEXT")
    private String operatorResponse;
    
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
        PENDING,
        IN_REVIEW,
        APPROVED,
        SENT,
        REJECTED
    }
    
    public enum Category {
        TECHNICAL,
        BILLING,
        ACCOUNT,
        GENERAL,
        OTHER
    }
}
