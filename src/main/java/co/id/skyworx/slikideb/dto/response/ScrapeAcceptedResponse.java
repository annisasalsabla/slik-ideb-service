package co.id.skyworx.slikideb.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Asynchronous acknowledgment response returned on POST /api/ideb/scrape (HTTP 202).
 */
@Data
@Builder
public class ScrapeAcceptedResponse {
    private String status;
    private String requestId;
    private String message;
    private String websocketTopic;
}
