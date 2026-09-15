package co.id.skyworx.slikideb.delegate;

import co.id.skyworx.slikideb.exception.ValidationException;
import co.id.skyworx.slikideb.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * BPMN Service Task delegate for validating scraped JSON payload integrity.
 */
@Component("validateDataDelegate")
@RequiredArgsConstructor
@Slf4j
public class ValidateDataDelegate implements JavaDelegate {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    public void execute(DelegateExecution execution) {
        String requestId = (String) execution.getVariable("requestId");
        log.info("[BPMN] Executing data validation task: requestId={}", requestId);
        notificationService.sendProgress(requestId, "VALIDATING");

        try {
            String scrapedDataJson = (String) execution.getVariable("scrapedDataJson");
            if (scrapedDataJson == null || scrapedDataJson.isBlank()) {
                throw new ValidationException("Scraped data payload is empty");
            }

            Map<String, Object> data = objectMapper.readValue(
                    scrapedDataJson, new TypeReference<>() {});

            validateField(data, "nik", "NIK");
            validateField(data, "nasabahName", "Customer Name");
            validateField(data, "statusKredit", "Credit Status");

            log.info("[BPMN] Data validation passed: requestId={}", requestId);

        } catch (ValidationException e) {
            log.error("[BPMN] Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[BPMN] Unexpected validation error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to validate scraped data: " + e.getMessage(), e);
        }
    }

    private void validateField(Map<String, Object> data, String key, String label) {
        Object value = data.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ValidationException("Field " + label + " must not be empty");
        }
    }
}
