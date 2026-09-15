package co.id.skyworx.slikideb.exception;

/**
 * Exception thrown when web scraping execution fails.
 */
public class ScrapingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ScrapingException(String message) {
        super(message);
    }
    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
