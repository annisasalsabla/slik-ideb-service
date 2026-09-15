package co.id.skyworx.slikideb.delegate;

import co.id.skyworx.slikideb.exception.PdfGenerationException;
import co.id.skyworx.slikideb.service.NotificationService;
import co.id.skyworx.slikideb.service.PdfGeneratorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * BPMN Service Task delegate for generating SLIK PDF reports.
 */
@Component("generatePdfDelegate")
@RequiredArgsConstructor
@Slf4j
public class GeneratePdfDelegate implements JavaDelegate {

    private final PdfGeneratorService pdfGeneratorService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    public void execute(DelegateExecution execution) {
        String requestId = (String) execution.getVariable("requestId");
        log.info("[BPMN] Executing PDF generation task: requestId={}", requestId);
        notificationService.sendProgress(requestId, "GENERATING_PDF");

        try {
            String scrapedDataJson = (String) execution.getVariable("scrapedDataJson");
            Map<String, Object> scrapedData = objectMapper.readValue(
                    scrapedDataJson, new TypeReference<>() {});

            Object[] result = pdfGeneratorService.generatePdf(scrapedData, requestId);
            byte[] pdfBytes = (byte[]) result[0];
            String filePath = (String) result[1];

            execution.setVariable("pdfBytes", pdfBytes);
            execution.setVariable("pdfFilePath", filePath);

            log.info("[BPMN] PDF generation completed: requestId={}, file={}", requestId, filePath);

        } catch (PdfGenerationException e) {
            log.error("[BPMN] PDF generation failed: requestId={}, error={}", requestId, e.getMessage());
            execution.setVariable("errorCode", "PDF_GENERATION_FAILED");
            execution.setVariable("errorMessage", e.getMessage());
            execution.setVariable("failedTask", "Generate PDF");
            // Triggers BPMN boundary error event to route to HandleErrorDelegate
            throw new BpmnError("PDF_GENERATION_FAILED", e.getMessage());

        } catch (Exception e) {
            log.error("[BPMN] Unexpected PDF generation error: requestId={}", requestId, e);
            execution.setVariable("errorCode", "PDF_GENERATION_FAILED");
            execution.setVariable("errorMessage", "Unexpected PDF error: " + e.getMessage());
            execution.setVariable("failedTask", "Generate PDF");
            throw new BpmnError("PDF_GENERATION_FAILED", "Unexpected PDF generation error");
        }
    }
}
