package co.id.skyworx.slikideb.delegate;

import co.id.skyworx.slikideb.entity.IdebFailureLog;
import co.id.skyworx.slikideb.repository.IdebFailureLogRepository;
import co.id.skyworx.slikideb.repository.IdebReportRepository;
import co.id.skyworx.slikideb.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BPMN Service Task delegate for handling and persisting workflow execution
 * failures.
 */
@Component("handleErrorDelegate")
@RequiredArgsConstructor
@Slf4j
public class HandleErrorDelegate implements JavaDelegate {

    private final IdebFailureLogRepository failureLogRepository;
    private final IdebReportRepository reportRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void execute(DelegateExecution execution) {
        String requestId = (String) execution.getVariable("requestId");
        String errorMessage = getVariableString(execution, "errorMessage", "Unknown error");
        String failedTask = getVariableString(execution, "failedTask", "Unknown task");
        String errorCode = getVariableString(execution, "errorCode", null);

        // Resolve errorCode if missing, blank, or UNKNOWN_ERROR
        if (errorCode == null || errorCode.isBlank() || "UNKNOWN_ERROR".equalsIgnoreCase(errorCode)) {
            if (failedTask.toLowerCase().contains("scrape")
                    || errorMessage.toLowerCase().contains("debtor data not found")
                    || errorMessage.toLowerCase().contains("scraping")) {
                errorCode = "SCRAPING_FAILED";
            } else if (failedTask.toLowerCase().contains("pdf")
                    || errorMessage.toLowerCase().contains("pdf")) {
                errorCode = "PDF_GENERATION_FAILED";
            } else {
                errorCode = "UNKNOWN_ERROR";
            }
        }

        log.error("[BPMN] Handling process failure: requestId={}, errorCode={}, task={}",
                requestId, errorCode, failedTask);

        IdebFailureLog failureLog = IdebFailureLog.builder()
                .processInstanceId(execution.getProcessInstanceId())
                .requestId(requestId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .failedTask(failedTask)
                .stacktraceSummary(errorMessage.length() > 500
                        ? errorMessage.substring(0, 500) + "..."
                        : errorMessage)
                .build();
        failureLogRepository.save(failureLog);

        reportRepository.findByRequestId(requestId).ifPresent(report -> {
            report.setStatus("FAILED");
            reportRepository.save(report);
        });

        notificationService.sendFailure(requestId, errorCode, "Process failed: " + errorMessage);
        log.info("[BPMN] Failure log persisted for requestId={}", requestId);
    }

    private String getVariableString(DelegateExecution execution, String varName, String defaultVal) {
        Object val = execution.getVariable(varName);
        return val != null ? val.toString() : defaultVal;
    }
}