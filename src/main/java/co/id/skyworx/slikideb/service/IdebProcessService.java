package co.id.skyworx.slikideb.service;

import co.id.skyworx.slikideb.dto.request.ScrapeRequest;
import co.id.skyworx.slikideb.entity.IdebReport;
import co.id.skyworx.slikideb.repository.IdebReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for triggering asynchronous Flowable BPMN process instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdebProcessService {

    private final RuntimeService runtimeService;
    private final IdebReportRepository reportRepository;

    /**
     * Starts the Flowable process instance asynchronously using background task executor.
     */
    @Async
    public void startProcess(String requestId, ScrapeRequest request) {
        log.info("Starting Flowable process instance: requestId={}", requestId);

        IdebReport initialRecord = IdebReport.builder()
                .requestId(requestId)
                .nik(request.getNik())
                .nasabahName(request.getNasabahName())
                .status("PROCESSING")
                .build();
        reportRepository.save(initialRecord);

        Map<String, Object> variables = new HashMap<>();
        variables.put("requestId", requestId);
        variables.put("nik", request.getNik() != null ? request.getNik() : "");
        variables.put("nasabahName", request.getNasabahName() != null ? request.getNasabahName() : "");

        try {
            var processInstance = runtimeService.startProcessInstanceByKey(
                    "idebReportProcess", requestId, variables);

            log.info("Process instance started: processInstanceId={}, requestId={}",
                    processInstance.getId(), requestId);

        } catch (Exception e) {
            log.error("Failed to start process instance: requestId={}, error={}", requestId, e.getMessage(), e);
            reportRepository.findByRequestId(requestId).ifPresent(report -> {
                report.setStatus("FAILED");
                reportRepository.save(report);
            });
        }
    }
}
