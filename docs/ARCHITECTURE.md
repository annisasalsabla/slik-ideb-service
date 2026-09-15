# System Architecture & Technical Notes

## Architecture Overview

```
Client (Hoppscotch / Browser)
    |
    | POST /api/ideb/scrape
    v
[IdebController] ──────────── HTTP 202 Accepted ──────────────────> Client
    |
    | @Async
    v
[IdebProcessService]
    |
    | startProcessInstanceByKey("idebReportProcess")
    v
[Flowable Engine] ─────── BPMN Process ──────────────────────────────────
    |                                                                     |
    |─── Service Task: Scrape Data ──── [ScrapeDataDelegate]             |
    |       |                                  |                          |
    |       | success                          | BpmnError                |
    |       v                                  v                          |
    |─── Service Task: Validate Data ── [ValidateDataDelegate]  [Error Boundary]
    |                                                                     |
    |─── Service Task: Generate PDF ─── [GeneratePdfDelegate]             |
    |       |                                  |                          |
    |       | success                          | BpmnError                |
    |       v                                  v                          |
    |─── Service Task: Save DB ──────── [SaveToDbDelegate]      [Error Boundary]
    |                                                                     |
    |─── Service Task: Notify Success ─ [NotifySuccessDelegate]          |
    |                                          |                          |
    |                              [WebSocket SUCCESS]            [handleErrorDelegate]
    v                                                                     |
[PostgreSQL]                                                    [WebSocket FAILED]
[ideb_reports]                                                  [ideb_failure_logs]
```

## Memory Management

PDF is rendered into an in-memory buffer (`ByteArrayOutputStream`) using try-with-resources before saving:

```java
try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
    renderer.createPDF(baos);
    pdfBytes = baos.toByteArray();
}
```

### Rationale:
1. **Atomicity**: Prevents corrupted or partial writes to disk/database if rendering fails midway.
2. **Dual-destination dispatch**: The in-memory byte array can be written simultaneously to the database (`bytea`) and disk without re-rendering.
3. **Clean lifecycle**: Avoids temporary file creation and manual disk cleanup.

*For very large documents (>10MB), stream directly to disk or cloud object storage (S3/GCS).*

## Error Handling Strategy

| Layer | Mechanism | Action |
|---|---|---|
| Controller | `@RestControllerAdvice` | Uniform JSON error responses |
| BPMN - Scraping | `BpmnError("SCRAPING_FAILED")` | Boundary Error Event -> Failure Log -> WebSocket FAILED |
| BPMN - PDF | `BpmnError("PDF_GENERATION_FAILED")` | Boundary Error Event -> Failure Log -> WebSocket FAILED |
| Validation | `@Valid` + `@AssertTrue` | HTTP 400 with field-level details |

## Troubleshooting

- **PostgreSQL Connection**: Verify PostgreSQL is running on port 5432 and database `slik_ideb_db` exists.
- **Playwright Chromium**: Install browser binaries with `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"`.
- **QueryDSL Q-Classes**: Run `mvn clean compile` to re-trigger annotation processor code generation.
- **Port Conflict**: Change `server.port` in `src/main/resources/application.yml` if port 8080 is in use.
