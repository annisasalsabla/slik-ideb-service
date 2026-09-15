package co.id.skyworx.slikideb.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Playwright bean configuration managing lifecycle of headless Chromium browser.
 */
@Configuration
@Slf4j
public class PlaywrightConfig {

    private Playwright playwright;
    private Browser browser;

    @Bean
    public Playwright playwright() {
        Playwright.CreateOptions options = new Playwright.CreateOptions();
        options.setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"));
        playwright = Playwright.create(options);
        return playwright;
    }

    @Bean
    public Browser chromiumBrowser(Playwright playwright) {
        log.info("Launching headless Chromium browser instance");
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(java.util.List.of(
                                "--no-sandbox",
                                "--disable-setuid-sandbox",
                                "--disable-dev-shm-usage"
                        ))
        );
        return browser;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Closing Playwright and browser instances");
        if (browser != null && browser.isConnected()) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
