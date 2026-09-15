package co.id.skyworx.slikideb;

import co.id.skyworx.slikideb.dto.request.IdebSearchRequest;
import co.id.skyworx.slikideb.dto.response.IdebReportResponse;
import co.id.skyworx.slikideb.dto.response.IdebReportSummaryDto;
import co.id.skyworx.slikideb.entity.IdebReport;
import co.id.skyworx.slikideb.repository.IdebReportRepository;
import co.id.skyworx.slikideb.repository.custom.IdebReportQueryRepository;
import co.id.skyworx.slikideb.service.IdebSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdebSearchServiceTest {

    @Mock
    private IdebReportQueryRepository queryRepository;

    @Mock
    private IdebReportRepository reportRepository;

    @InjectMocks
    private IdebSearchService searchService;

    @Test
    @DisplayName("Should return paged response with correctly mapped DTOs")
    void testSearchReports_ReturnsPagedResponse() {
        // Arrange
        IdebReportSummaryDto mockDto = new IdebReportSummaryDto(
                1L,
                "req-123",
                "Budi Santoso",
                "LANCAR",
                new BigDecimal("150000000"),
                LocalDateTime.now()
        );

        Page<IdebReportSummaryDto> mockPage = new PageImpl<>(
                List.of(mockDto),
                PageRequest.of(0, 10),
                1L
        );

        when(queryRepository.searchReports(anyString(), anyString(), anyString(),
                any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        IdebSearchRequest request = new IdebSearchRequest();
        request.setNasabahName("Budi");
        request.setStatusKredit("LANCAR");
        request.setNik("3174012501900001");
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("createdAt");
        request.setSortDir("desc");

        // Act
        Page<IdebReportSummaryDto> result = searchService.searchReports(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        IdebReportSummaryDto response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRequestId()).isEqualTo("req-123");
        assertThat(response.getNasabahName()).isEqualTo("Budi Santoso");
        assertThat(response.getStatusKredit()).isEqualTo("LANCAR");
        assertThat(response.getNominalTagihan()).isEqualByComparingTo(new BigDecimal("150000000"));
        assertThat(response.getDownloadUrl()).contains("/api/ideb/report/1/download");
        assertThat(response.getUrl()).contains("/api/ideb/report/1/download");

        verify(queryRepository).searchReports(
                eq("Budi"), eq("3174012501900001"), eq("LANCAR"),
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle empty query filters without throwing exceptions")
    void testSearchReports_EmptyFilter_NoError() {
        Page<IdebReportSummaryDto> emptyPage = new PageImpl<>(
                List.of(), PageRequest.of(0, 10), 0L);

        when(queryRepository.searchReports(isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        IdebSearchRequest request = new IdebSearchRequest();

        Page<IdebReportSummaryDto> result = searchService.searchReports(request);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should omit heavy byte array from response DTO")
    void testToResponse_NoPdfContent() {
        IdebReport report = IdebReport.builder()
                .id(1L)
                .requestId("req-abc")
                .nik("3174012501900001")
                .pdfContent(new byte[]{1, 2, 3})
                .status("SUCCESS")
                .build();

        IdebReportResponse response = searchService.toResponse(report);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDownloadUrl()).contains("/api/ideb/report/1/download");
    }
}
