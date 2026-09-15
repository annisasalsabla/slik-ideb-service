package co.id.skyworx.slikideb.exception;

public class PdfGenerationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PdfGenerationException(String message) {
        super(message);
    }
    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
