package co.id.skyworx.slikideb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity persisting workflow task execution failures and error details.
 */
@Entity
@Table(name = "ideb_failure_logs", indexes = {
    @Index(name = "idx_failure_request_id", columnList = "requestId"),
    @Index(name = "idx_failure_error_code", columnList = "errorCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdebFailureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String processInstanceId;

    @Column(nullable = false)
    private String requestId;

    @Column(length = 100)
    private String errorCode;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 200)
    private String failedTask;

    @Column(columnDefinition = "TEXT")
    private String stacktraceSummary;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
