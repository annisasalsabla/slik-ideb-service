package co.id.skyworx.slikideb;

import co.id.skyworx.slikideb.exception.ScrapingException;
import co.id.skyworx.slikideb.service.ScrapingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapingServiceTest {

    @Mock
    private Browser browser;

    @Mock
    private BrowserContext browserContext;

    @Mock
    private Page page;

    @Mock
    private ElementHandle matchingRow;

    @Mock
    private ElementHandle statusEl;

    @Mock
    private ElementHandle nominalEl;

    @Mock
    private ElementHandle bankEl;

    @Mock
    private ElementHandle jatuhTempoEl;

    @Mock
    private ElementHandle kolektibilitasEl;

    @InjectMocks
    private ScrapingService scrapingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scrapingService, "targetUrl",
                "http://localhost:8080/mock/slik-data");
        ReflectionTestUtils.setField(scrapingService, "timeoutMs", 5000);
        ReflectionTestUtils.setField(scrapingService, "objectMapper", new ObjectMapper());
    }

    @Test
    @DisplayName("Should extract debtor information when matching by NIK")
    void testScrapeByNik_Success() {
        when(browser.newContext()).thenReturn(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.isClosed()).thenReturn(false);
        when(page.querySelectorAll("#table-slik tbody tr"))
                .thenReturn(List.of(matchingRow));
        when(matchingRow.getAttribute("data-nik")).thenReturn("3174012501900001");
        when(matchingRow.getAttribute("data-nama")).thenReturn("Budi Santoso");

        when(matchingRow.querySelector(".col-status")).thenReturn(statusEl);
        when(matchingRow.querySelector(".col-nominal")).thenReturn(nominalEl);
        when(matchingRow.querySelector(".col-bank")).thenReturn(bankEl);
        when(matchingRow.querySelector(".col-jatuh-tempo")).thenReturn(jatuhTempoEl);
        when(matchingRow.querySelector(".col-kolektibilitas")).thenReturn(kolektibilitasEl);

        when(statusEl.textContent()).thenReturn("LANCAR");
        when(nominalEl.textContent()).thenReturn("150,000,000");
        when(bankEl.textContent()).thenReturn("Bank Mandiri");
        when(jatuhTempoEl.textContent()).thenReturn("15/12/2026");
        when(kolektibilitasEl.textContent()).thenReturn("1");

        Map<String, Object> result = scrapingService.scrapeNasabahData("3174012501900001", null);

        assertThat(result).isNotNull();
        assertThat(result.get("nik")).isEqualTo("3174012501900001");
        assertThat(result.get("nasabahName")).isEqualTo("Budi Santoso");
        assertThat(result.get("statusKredit")).isEqualTo("LANCAR");
        assertThat(result.get("namaBank")).isEqualTo("Bank Mandiri");

        verify(page).close();
    }

    @Test
    @DisplayName("Should throw ScrapingException when debtor NIK is not found")
    void testScrapeByNik_NotFound() {
        when(browser.newContext()).thenReturn(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.isClosed()).thenReturn(false);
        when(page.querySelectorAll("#table-slik tbody tr"))
                .thenReturn(List.of(matchingRow));
        when(matchingRow.getAttribute("data-nik")).thenReturn("9999999999999999");
        when(matchingRow.getAttribute("data-nama")).thenReturn("Orang Lain");

        assertThatThrownBy(() ->
                scrapingService.scrapeNasabahData("3174012501900001", null))
                .isInstanceOf(ScrapingException.class);

        verify(page).close();
    }

    @Test
    @DisplayName("Should throw ScrapingException when target table is empty")
    void testScrape_EmptyTable() {
        when(browser.newContext()).thenReturn(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.isClosed()).thenReturn(false);
        when(page.querySelectorAll("#table-slik tbody tr")).thenReturn(List.of());

        assertThatThrownBy(() ->
                scrapingService.scrapeNasabahData("3174012501900001", null))
                .isInstanceOf(ScrapingException.class);

        verify(page).close();
    }

    @Test
    @DisplayName("Should match debtor by case-insensitive partial name")
    void testScrapeByName_PartialMatch() {
        when(browser.newContext()).thenReturn(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.isClosed()).thenReturn(false);
        when(page.querySelectorAll("#table-slik tbody tr"))
                .thenReturn(List.of(matchingRow));
        when(matchingRow.getAttribute("data-nik")).thenReturn("3174012501900001");
        when(matchingRow.getAttribute("data-nama")).thenReturn("Budi Santoso");
        when(matchingRow.querySelector(".col-status")).thenReturn(statusEl);
        when(matchingRow.querySelector(".col-nominal")).thenReturn(nominalEl);
        when(matchingRow.querySelector(".col-bank")).thenReturn(bankEl);
        when(matchingRow.querySelector(".col-jatuh-tempo")).thenReturn(jatuhTempoEl);
        when(matchingRow.querySelector(".col-kolektibilitas")).thenReturn(kolektibilitasEl);
        when(statusEl.textContent()).thenReturn("LANCAR");
        when(nominalEl.textContent()).thenReturn("150,000,000");
        when(bankEl.textContent()).thenReturn("Bank Mandiri");
        when(jatuhTempoEl.textContent()).thenReturn("15/12/2026");
        when(kolektibilitasEl.textContent()).thenReturn("1");

        Map<String, Object> result = scrapingService.scrapeNasabahData(null, "budi");

        assertThat(result.get("nasabahName")).isEqualTo("Budi Santoso");
    }
}
