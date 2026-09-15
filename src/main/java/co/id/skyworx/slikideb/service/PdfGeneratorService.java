package co.id.skyworx.slikideb.service;

import co.id.skyworx.slikideb.exception.PdfGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for rendering debtor information into PDF documents using Flying Saucer and Thymeleaf.
 */
@Service
@Slf4j
public class PdfGeneratorService {

    private final TemplateEngine templateEngine;

    public PdfGeneratorService(@Qualifier("pdfTemplateEngine") TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Value("${app.pdf.output-dir:D:/Projects/slik-ideb-service/pdf-output}")
    private String pdfOutputDir;

    @Value("${app.pdf.simulate-failure:false}")
    private boolean simulateFailure;

    /**
     * Generates a PDF report from scraped debtor data.
     * Returns an array containing the in-memory byte array and saved file path.
     */
    public Object[] generatePdf(Map<String, Object> scrapedData, String requestId) {
        if (simulateFailure) {
            log.warn("Simulated PDF failure triggered (app.pdf.simulate-failure=true)");
            throw new PdfGenerationException("Simulated PDF failure for BPMN Error Boundary testing");
        }

        log.info("Generating PDF report: requestId={}", requestId);

        try {
            String htmlContent = renderHtmlTemplate(scrapedData, requestId);

            // In-memory buffering ensures atomic write and dual destination persistence (DB + disk)
            byte[] pdfBytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(htmlContent);
                renderer.layout();
                renderer.createPDF(baos);
                pdfBytes = baos.toByteArray();
            }

            log.info("PDF rendered in-memory, size: {} bytes", pdfBytes.length);
            String filePath = savePdfToFile(pdfBytes, scrapedData, requestId);
            log.info("PDF saved to disk: {}", filePath);

            return new Object[]{pdfBytes, filePath};

        } catch (PdfGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF generation failed: requestId={}, error={}", requestId, e.getMessage(), e);
            throw new PdfGenerationException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private String renderHtmlTemplate(Map<String, Object> data, String requestId) {
        Context context = new Context();
        context.setVariables(data);
        context.setVariable("requestId", requestId);
        context.setVariable("generatedAt",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        return templateEngine.process("slik-report-template", context);
    }

    private String savePdfToFile(byte[] pdfBytes, Map<String, Object> data, String requestId) throws Exception {
        Path outputDir = Paths.get(pdfOutputDir);
        Files.createDirectories(outputDir);

        String nik = data.getOrDefault("nik", "UNKNOWN").toString();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("SLIK_%s_%s.pdf", nik, timestamp);
        Path filePath = outputDir.resolve(fileName);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(pdfBytes);
        }

        return filePath.toAbsolutePath().toString();
    }
}
