package co.id.skyworx.slikideb.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing an IDEB report without heavy binary payload.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdebReportResponse {
    private Long id;
    private String requestId;
    private String nik;
    private String nasabahName;
    private String statusKredit;
    private BigDecimal nominalTagihan;
    private String namaBank;
    private String kolektibilitas;
    private String pdfPath;
    private String processInstanceId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String downloadUrl;
}
