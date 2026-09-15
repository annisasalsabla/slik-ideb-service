package co.id.skyworx.slikideb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller serving the mock SLIK web portal targeted by the Playwright scraper.
 */
@Controller
@RequestMapping("/mock")
public class MockPageController {

    @GetMapping("/slik-data")
    public String mockSlikPage() {
        return "mock-slik-data";
    }
}
