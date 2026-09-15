package co.id.skyworx.slikideb.controller;

import co.id.skyworx.slikideb.dto.request.IdebSearchRequest;
import co.id.skyworx.slikideb.dto.request.ScrapeRequest;
import co.id.skyworx.slikideb.dto.response.*;
import co.id.skyworx.slikideb.entity.IdebFailureLog;
import co.id.skyworx.slikideb.entity.IdebReport;
import co.id.skyworx.slikideb.repository.IdebFailureLogRepository;
import co.id.skyworx.slikideb.service.IdebProcessService;
import co.id.skyworx.slikideb.service.IdebSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller utama untuk endpoint SLIK/IDEB.
 */
@RestController
@RequestMapping("/api/ideb")
@RequiredArgsConstructor
@Slf4j
public class IdebController {

    private final IdebProcessService processService;
    private final IdebSearchService searchService;
    private final IdebFailureLogRepository failureLogRepository;

    /**
     * Initiates asynchronous SLIK report generation via Flowable workflow.
     */
    @PostMapping("/scrape")
    public ResponseEntity<ApiResponse<ScrapeAcceptedResponse>> startScrape(
            @Valid @RequestBody ScrapeRequest request) {

        String requestId = UUID.randomUUID().toString();
        log.info("Received scrape request: requestId={}, nik={}, name={}",
                requestId, request.getNik(), request.getNasabahName());

        processService.startProcess(requestId, request);

        ScrapeAcceptedResponse response = ScrapeAcceptedResponse.builder()
                .status("ACCEPTED")
                .requestId(requestId)
                .message("SLIK report generation is in progress. Monitor updates via WebSocket.")
                .websocketTopic("/topic/ideb/" + requestId)
                .build();

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.accepted(response, "Request accepted, processing in background"));
    }

    /**
     * Searches IDEB reports with dynamic query filters.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<IdebReportResponse>>> searchReports(
            @ModelAttribute IdebSearchRequest request) {

        Page<IdebReportResponse> result = searchService.searchReports(request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Found " + result.getTotalElements() + " reports"));
    }

    /**
     * Downloads generated SLIK PDF report as an attachment.
     */
    @GetMapping("/report/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadReport(@PathVariable Long id) {
        IdebReport report = searchService.findById(id);

        if (report.getPdfContent() == null || report.getPdfContent().length == 0) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(report.getPdfContent());
        String fileName = String.format("SLIK_%s_%s.pdf",
                report.getNik() != null ? report.getNik() : "UNKNOWN",
                report.getRequestId().substring(0, 8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(report.getPdfContent().length)
                .body(resource);
    }

    /**
     * Retrieves failure execution logs.
     */
    @GetMapping("/failures")
    public ResponseEntity<ApiResponse<List<IdebFailureLog>>> getFailureLogs() {
        List<IdebFailureLog> logs = failureLogRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.ok(logs, "Retrieved " + logs.size() + " failure logs"));
    }

    /**
     * Retrieves report execution status by requestId.
     */
    @GetMapping("/report/{requestId}/status")
    public ResponseEntity<ApiResponse<IdebReportResponse>> getProcessStatus(
            @PathVariable String requestId) {
        return searchService.findByRequestIdOptional(requestId)
                .map(report -> ResponseEntity.ok(
                        ApiResponse.ok(searchService.toResponse(report), "Status retrieved")))
                .orElse(ResponseEntity.notFound().build());
    }
}
