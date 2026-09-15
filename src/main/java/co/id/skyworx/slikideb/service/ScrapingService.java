package co.id.skyworx.slikideb.service;

import co.id.skyworx.slikideb.exception.ScrapingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for web scraping debtor data using Playwright.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingService {

    private final Browser browser;
    private final ObjectMapper objectMapper;

    @Value("${app.scraping.target-url}")
    private String targetUrl;

    @Value("${app.scraping.timeout-ms:30000}")
    private int timeoutMs;

    /**
     * Scrapes debtor data matching either NIK or customer name from mock portal.
     */
    public Map<String, Object> scrapeNasabahData(String nik, String nasabahName) {
        log.info("Starting scrape: nik={}, name={}", nik, nasabahName);

        // Isolated context per request; auto-closed by try-with-resources to prevent memory leaks
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            try {
                page.navigate(targetUrl);
                page.waitForSelector("#table-slik tbody tr",
                        new Page.WaitForSelectorOptions().setTimeout(timeoutMs));

                List<ElementHandle> rows = page.querySelectorAll("#table-slik tbody tr");
                if (rows.isEmpty()) {
                    throw new ScrapingException("SLIK table is empty or missing");
                }

                for (ElementHandle row : rows) {
                    String rowNik = row.getAttribute("data-nik");
                    String rowNama = row.getAttribute("data-nama");

                    boolean nikMatch = nik != null && !nik.isBlank() && nik.equals(rowNik);
                    boolean namaMatch = nasabahName != null && !nasabahName.isBlank()
                            && rowNama != null
                            && rowNama.toLowerCase().contains(nasabahName.toLowerCase());

                    if (nikMatch || namaMatch) {
                        Map<String, Object> data = extractRowData(row);
                        log.info("Debtor matched: nik={}, name={}", data.get("nik"), data.get("nasabahName"));
                        return data;
                    }
                }

                String searchInfo = nik != null ? "NIK=" + nik : "Name=" + nasabahName;
                throw new ScrapingException("Debtor data not found for " + searchInfo);

            } finally {
                // Ensure page resources are released even on error
                if (!page.isClosed()) {
                    page.close();
                }
            }
        } catch (ScrapingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Scraping execution failed: {}", e.getMessage(), e);
            throw new ScrapingException("Failed to access source portal: " + e.getMessage(), e);
        }
    }

    /**
     * Ekstrak data dari satu baris tabel ke Map.
     * Menggunakan selector CSS berdasarkan class yang didefinisikan di mock HTML.
     */
    private Map<String, Object> extractRowData(ElementHandle row) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("nik", row.getAttribute("data-nik"));
        data.put("nasabahName", row.getAttribute("data-nama"));
        data.put("statusKredit", getTextByClass(row, ".col-status"));
        data.put("nominalTagihan", getTextByClass(row, ".col-nominal"));
        data.put("namaBank", getTextByClass(row, ".col-bank"));
        data.put("tanggalJatuhTempo", getTextByClass(row, ".col-jatuh-tempo"));
        data.put("kolektibilitas", getTextByClass(row, ".col-kolektibilitas"));
        data.put("scrapedAt", java.time.LocalDateTime.now().toString());

        return data;
    }

    private String getTextByClass(ElementHandle row, String cssClass) {
        ElementHandle el = row.querySelector(cssClass);
        return el != null ? el.textContent().trim() : "";
    }
}
