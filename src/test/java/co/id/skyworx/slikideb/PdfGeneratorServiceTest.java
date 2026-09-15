package co.id.skyworx.slikideb;

import co.id.skyworx.slikideb.exception.PdfGenerationException;
import co.id.skyworx.slikideb.service.PdfGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfGeneratorServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private PdfGeneratorService pdfGeneratorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pdfGeneratorService, "pdfOutputDir",
                System.getProperty("java.io.tmpdir") + "/test-pdf-output");
        ReflectionTestUtils.setField(pdfGeneratorService, "simulateFailure", false);
    }

    @Test
    @DisplayName("Should throw PdfGenerationException when simulate-failure flag is active")
    void testGeneratePdf_SimulateFailure() {
        ReflectionTestUtils.setField(pdfGeneratorService, "simulateFailure", true);

        Map<String, Object> data = new HashMap<>();
        data.put("nik", "3174012501900001");
        data.put("nasabahName", "Budi Santoso");

        assertThatThrownBy(() ->
                pdfGeneratorService.generatePdf(data, "test-request-id"))
                .isInstanceOf(PdfGenerationException.class)
                .hasMessageContaining("Simulated PDF failure");

        verifyNoInteractions(templateEngine);
    }

    @Test
    @DisplayName("Should execute template engine and return generated PDF bytes")
    void testGeneratePdf_CallsTemplateEngine() {
        // Arrange
        Map<String, Object> data = new HashMap<>();
        data.put("nik", "3174012501900001");
        data.put("nasabahName", "Budi Santoso");
        data.put("statusKredit", "LANCAR");
        data.put("nominalTagihan", "150,000,000");
        data.put("namaBank", "Bank Mandiri");
        data.put("tanggalJatuhTempo", "15/12/2026");
        data.put("kolektibilitas", "1");

        // Mock template engine mengembalikan HTML minimal valid untuk Flying Saucer
        String minimalHtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>Test</title></head>" +
                "<body><p>Test PDF</p></body></html>";

        when(templateEngine.process(eq("slik-report-template"), any(Context.class)))
                .thenReturn(minimalHtml);

        // Act
        Object[] result = pdfGeneratorService.generatePdf(data, "test-request-id");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result[0]).isInstanceOf(byte[].class);
        assertThat((byte[]) result[0]).isNotEmpty();
        assertThat(result[1]).isInstanceOf(String.class);
        assertThat((String) result[1]).contains("SLIK_3174012501900001");

        verify(templateEngine).process(eq("slik-report-template"), any(Context.class));
    }
}
