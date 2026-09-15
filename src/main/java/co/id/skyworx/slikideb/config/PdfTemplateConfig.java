package co.id.skyworx.slikideb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Dedicated Thymeleaf template engine for PDF rendering.
 * Uses XML template mode to ensure strict XHTML compliance required by Flying Saucer.
 */
@Configuration
public class PdfTemplateConfig {

    @Bean(name = "pdfTemplateEngine")
    public TemplateEngine pdfTemplateEngine() {
        TemplateEngine engine = new TemplateEngine();
        engine.addTemplateResolver(pdfTemplateResolver());
        return engine;
    }

    private ClassLoaderTemplateResolver pdfTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        // Lokasi template PDF di classpath (resources/pdf-templates/)
        resolver.setPrefix("pdf-templates/");
        resolver.setSuffix(".html");
        // Mode XML memastikan output XHTML-valid untuk Flying Saucer
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setOrder(1);
        return resolver;
    }
}
