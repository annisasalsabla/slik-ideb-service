package co.id.skyworx.slikideb.service;

import co.id.skyworx.slikideb.dto.request.IdebSearchRequest;
import co.id.skyworx.slikideb.dto.response.IdebReportResponse;
import co.id.skyworx.slikideb.dto.response.IdebReportSummaryDto;
import co.id.skyworx.slikideb.entity.IdebReport;
import co.id.skyworx.slikideb.repository.IdebReportRepository;
import co.id.skyworx.slikideb.repository.custom.IdebReportQueryRepository;
import co.id.skyworx.slikideb.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for querying and retrieving IDEB reports using QueryDSL predicates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdebSearchService {

    private final IdebReportQueryRepository queryRepository;
    private final IdebReportRepository reportRepository;

    public Page<IdebReportSummaryDto> searchReports(IdebSearchRequest request) {
        log.debug("Searching reports: name={}, nik={}, status={}",
                request.getNasabahName(), request.getNik(), request.getStatusKredit());

        Sort sort = "asc".equalsIgnoreCase(request.getSortDir())
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        return queryRepository.searchReports(
                request.getNasabahName(),
                request.getNik(),
                request.getStatusKredit(),
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public IdebReport findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ValidationException(
                        "Report not found with ID: " + id));
    }

    public Optional<IdebReport> findByRequestIdOptional(String requestId) {
        return reportRepository.findByRequestId(requestId);
    }

    public IdebReportResponse toResponse(IdebReport report) {
        return IdebReportResponse.builder()
                .id(report.getId())
                .requestId(report.getRequestId())
                .nik(report.getNik())
                .nasabahName(report.getNasabahName())
                .statusKredit(report.getStatusKredit())
                .nominalTagihan(report.getNominalTagihan())
                .namaBank(report.getNamaBank())
                .kolektibilitas(report.getKolektibilitas())
                .pdfPath(report.getPdfPath())
                .processInstanceId(report.getProcessInstanceId())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .downloadUrl(report.getId() != null
                        ? "http://localhost:8080/api/ideb/report/" + report.getId() + "/download"
                        : null)
                .build();
    }
}
