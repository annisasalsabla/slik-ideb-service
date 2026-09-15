package co.id.skyworx.slikideb.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * Request payload for POST /api/ideb/scrape.
 */
@Data
public class ScrapeRequest {

    private String nik;
    private String nasabahName;

    @AssertTrue(message = "Either NIK or Customer Name must be provided")
    public boolean isAtLeastOneFieldProvided() {
        return (nik != null && !nik.isBlank()) ||
               (nasabahName != null && !nasabahName.isBlank());
    }
}
