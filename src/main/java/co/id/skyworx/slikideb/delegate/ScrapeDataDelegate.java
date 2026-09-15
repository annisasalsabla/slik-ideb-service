package co.id.skyworx.slikideb.delegate;

import co.id.skyworx.slikideb.exception.ScrapingException;
import co.id.skyworx.slikideb.service.NotificationService;
import co.id.skyworx.slikideb.service.ScrapingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * BPMN Service Task delegate for external web scraping.
 */
@Component("scrapeDataDelegate")
@RequiredArgsConstructor
@Slf4j
public class ScrapeDataDelegate implements JavaDelegate {

    private final ScrapingService scrapingService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) {
        String requestId = (String) execution.getVariable("requestId");
        String nik = (String) execution.getVariable("nik");
        String nasabahName = (String) execution.getVariable("nasabahName");

        log.info("[BPMN] Executing scrape task: requestId={}", requestId);
        notificationService.sendProgress(requestId, "SCRAPING");

        try {
            Map<String, Object> scrapedData = scrapingService.scrapeNasabahData(nik, nasabahName);
            String scrapedDataJson = objectMapper.writeValueAsString(scrapedData);

            execution.setVariable("scrapedDataJson", scrapedDataJson);
            execution.setVariable("scrapedNik", scrapedData.getOrDefault("nik", "").toString());
            execution.setVariable("scrapedNasabahName", scrapedData.getOrDefault("nasabahName", "").toString());

            log.info("[BPMN] Scrape task completed: requestId={}", requestId);

        } catch (ScrapingException e) {
            log.error("[BPMN] Scraping failed: requestId={}, error={}", requestId, e.getMessage());
            execution.setVariable("errorCode", "SCRAPING_FAILED");
            execution.setVariable("errorMessage", e.getMessage());
            execution.setVariable("failedTask", "Scrape Data Eksternal");
            // Triggers BPMN boundary error event to route to HandleErrorDelegate
            throw new BpmnError("SCRAPING_FAILED", e.getMessage());

        } catch (Exception e) {
            log.error("[BPMN] Unexpected error during scraping: requestId={}", requestId, e);
            execution.setVariable("errorCode", "SCRAPING_FAILED");
            execution.setVariable("errorMessage", "Unexpected error: " + e.getMessage());
            execution.setVariable("failedTask", "Scrape Data Eksternal");
            throw new BpmnError("SCRAPING_FAILED", "Unexpected scraping error: " + e.getMessage());
        }
    }
}
