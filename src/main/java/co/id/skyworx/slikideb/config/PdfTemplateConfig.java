package co.id.skyworx.slikideb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Dedicated Thymeleaf template engine for PDF rendering.
 * Uses XML template mode to ensure strict XHTML compliance required by Flying
 * Saucer.
 */
@Configuration
public class PdfTemplateConfig {

    @Bean(name = "pdfTemplateEngine")
    public TemplateEngine pdfTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(pdfTemplateResolver());
        return engine;
    }

    private ClassLoaderTemplateResolver pdfTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        // Dedicated PDF template location in classpath (resources/templates/)
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        // Mode XML memastikan output XHTML-valid untuk Flying Saucer
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setOrder(1);
        return resolver;
    }
}