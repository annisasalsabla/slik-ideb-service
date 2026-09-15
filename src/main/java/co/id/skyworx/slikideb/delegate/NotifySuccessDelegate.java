package co.id.skyworx.slikideb.delegate;

import co.id.skyworx.slikideb.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * BPMN Service Task delegate for dispatching successful completion notifications via WebSocket.
 */
@Component("notifySuccessDelegate")
@RequiredArgsConstructor
@Slf4j
public class NotifySuccessDelegate implements JavaDelegate {

    private final NotificationService notificationService;

    @Override
    public void execute(DelegateExecution execution) {
        String requestId = (String) execution.getVariable("requestId");
        Long reportId = (Long) execution.getVariable("reportId");

        log.info("[BPMN] Dispatching success notification: requestId={}, reportId={}", requestId, reportId);
        notificationService.sendSuccess(requestId, reportId);
        log.info("[BPMN] Workflow completed successfully for requestId={}", requestId);
    }
}
