package co.id.skyworx.slikideb.delegate;

import co.id.skyworx.slikideb.entity.IdebReport;
import co.id.skyworx.slikideb.repository.IdebReportRepository;
import co.id.skyworx.slikideb.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * BPMN Service Task delegate for persisting scraped IDEB reports and generated PDF bytes.
 */
@Component("saveToDbDelegate")
@RequiredArgsConstructor
@Slf4j
public class SaveToDbDelegate implements JavaDelegate {

    private final IdebReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    public void execute(DelegateExecution execution) {
        String requestId = (String) execution.getVariable("requestId");
        log.info("[BPMN] Executing database persistence task: requestId={}", requestId);
        notificationService.sendProgress(requestId, "SAVING");

        try {
            String scrapedDataJson = (String) execution.getVariable("scrapedDataJson");
            byte[] pdfBytes = (byte[]) execution.getVariable("pdfBytes");
            String pdfFilePath = (String) execution.getVariable("pdfFilePath");

            Map<String, Object> data = objectMapper.readValue(
                    scrapedDataJson, new TypeReference<>() {});

            BigDecimal nominal = parseNominal(data.getOrDefault("nominalTagihan", "0").toString());

            IdebReport report = reportRepository.findByRequestId(requestId)
                    .orElse(IdebReport.builder().requestId(requestId).build());

            report.setNik(data.getOrDefault("nik", "").toString());
            report.setNasabahName(data.getOrDefault("nasabahName", "").toString());
            report.setStatusKredit(data.getOrDefault("statusKredit", "").toString());
            report.setNominalTagihan(nominal);
            report.setNamaBank(data.getOrDefault("namaBank", "").toString());
            report.setKolektibilitas(data.getOrDefault("kolektibilitas", "").toString());
            report.setRawJson(scrapedDataJson);
            report.setPdfContent(pdfBytes);
            report.setPdfPath(pdfFilePath);
            report.setProcessInstanceId(execution.getProcessInstanceId());
            report.setStatus("SUCCESS");

            IdebReport saved = reportRepository.save(report);
            execution.setVariable("reportId", saved.getId());

            log.info("[BPMN] Persistence completed: requestId={}, reportId={}", requestId, saved.getId());

        } catch (Exception e) {
            log.error("[BPMN] Database persistence error: requestId={}", requestId, e);
            throw new RuntimeException("Failed to persist report to database: " + e.getMessage(), e);
        }
    }

    private BigDecimal parseNominal(String nominalStr) {
        try {
            String cleaned = nominalStr.replaceAll("[^0-9.,]", "").replace(",", "");
            return new BigDecimal(cleaned.isEmpty() ? "0" : cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
