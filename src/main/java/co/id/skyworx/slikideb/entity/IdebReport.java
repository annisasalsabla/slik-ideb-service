package co.id.skyworx.slikideb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an individual debtor's IDEB report.
 */
@Entity
@Table(name = "ideb_reports", indexes = {
    @Index(name = "idx_ideb_nik", columnList = "nik"),
    @Index(name = "idx_ideb_request_id", columnList = "requestId"),
    @Index(name = "idx_ideb_status_kredit", columnList = "statusKredit"),
    @Index(name = "idx_ideb_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdebReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String requestId;

    @Column(length = 16)
    private String nik;

    @Column(length = 200)
    private String nasabahName;

    @Column(length = 50)
    private String statusKredit;

    @Column(precision = 15, scale = 2)
    private BigDecimal nominalTagihan;

    @Column(length = 200)
    private String namaBank;

    @Column(length = 10)
    private String kolektibilitas;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

    @Column(length = 500)
    private String pdfPath;

    @Lob
    @Column(name = "pdf_content")
    private byte[] pdfContent;

    @Column(length = 100)
    private String processInstanceId;

    @Column(length = 20)
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
