package backend.entity;

import backend.entity.enums.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GenerationType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Lob;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "organization")
    private String organization;

    @Column(name = "fio")
    private String fio;

    @Column(name = "phone")
    private String phone;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "serial_number")
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private String category;

    @Column(name = "confidence_score")
    private Float confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "project")
    private String project;

    @Column(name = "inn")
    private String inn;

    @Column(name = "country_region")
    private String countryRegion;

    @Lob
    @Column(name = "file")
    private byte[] file;

    @Column(name = "ai_generated_answer", columnDefinition = "TEXT")
    private String aiGeneratedAnswer;

    @Column(name = "operator_answer", columnDefinition = "TEXT")
    private String operatorAnswer;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime  updatedAt;
}
