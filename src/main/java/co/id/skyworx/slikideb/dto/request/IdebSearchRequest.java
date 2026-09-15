package co.id.skyworx.slikideb.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameters for GET /api/ideb/search.
 */
@Data
public class IdebSearchRequest {

    private String nasabahName;
    private String nik;
    private String statusKredit;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
