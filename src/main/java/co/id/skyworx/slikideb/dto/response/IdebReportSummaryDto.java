package co.id.skyworx.slikideb.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Summary DTO for IDEB search results, excluding heavy LOB columns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdebReportSummaryDto {

    private Long id;
    private String requestId;
    private String nasabahName;
    private String statusKredit;
    private BigDecimal nominalTagihan;
    private LocalDateTime createdAt;
    private String downloadUrl;
    private String url;

    /**
     * Constructor for QueryDSL Projections.constructor.
     */
    public IdebReportSummaryDto(Long id, String requestId, String nasabahName,
                               String statusKredit, BigDecimal nominalTagihan,
                               LocalDateTime createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.nasabahName = nasabahName;
        this.statusKredit = statusKredit;
        this.nominalTagihan = nominalTagihan;
        this.createdAt = createdAt;
        String link = id != null
                ? "http://localhost:8080/api/ideb/report/" + id + "/download"
                : null;
        this.downloadUrl = link;
        this.url = link;
    }
}
