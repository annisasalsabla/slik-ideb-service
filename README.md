# SLIK IDEB Service

Microservice untuk otomatisasi proses verifikasi data nasabah dan pembuatan laporan SLIK/IDEB berbasis alur kerja Flowable BPMN, Playwright web scraping, dan rendering PDF.

## Tech Stack

- **Runtime & Framework:** Java 17, Spring Boot 3.3.5
- **Workflow Engine:** Flowable 7.0.1 (BPMN 2.0)
- **Web Scraping:** Microsoft Playwright for Java
- **PDF Engine:** Flying Saucer (OpenPDF) + Thymeleaf
- **Database & Query:** PostgreSQL, Spring Data JPA, QueryDSL 5.1.0 (Jakarta)
- **Real-time Messaging:** Spring WebSocket (STOMP + SockJS)

## Cara Menjalankan

1. Setup database PostgreSQL `slik_ideb_db` (lihat skrip DDL di `docs/database-setup.sql`).
2. Install browser binary Playwright (hanya sekali di awal):
   ```bash
   mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
   ```
3. Build project:
   ```bash
   mvn clean compile
   ```
4. Jalankan aplikasi:
   ```bash
   mvn spring-boot:run
   ```
5. Buka real-time tester di browser:
   `http://localhost:8080/ws-tester.html`

## Endpoint Utama

| Method | Path | Deskripsi |
|---|---|---|
| `POST` | `/api/ideb/scrape` | Menerima request verifikasi dan memicu proses BPMN secara async (HTTP 202). |
| `GET` | `/api/ideb/search` | Pencarian laporan IDEB dinamis dengan filter nama, NIK, status, tanggal, dan paginasi. |
| `GET` | `/api/ideb/report/{id}/download` | Mengunduh file PDF laporan SLIK hasil generate. |
| `GET` | `/api/ideb/report/{requestId}/status` | Memeriksa status proses report berdasarkan requestId. |
| `GET` | `/api/ideb/failures` | Menampilkan riwayat log kegagalan proses scraping/PDF. |
| `GET` | `/actuator/health` | Health check endpoint aplikasi dan database. |

## Alur BPMN

```
[Start] -> [Scrape Data] -> [Validasi] -> [Generate PDF] -> [Simpan DB] -> [Notif Sukses] -> [End]
                 |                             |
                 | SCRAPING_FAILED             | PDF_GENERATION_FAILED
                 v                             v
             [Catat Log Kegagalan] <-----------+
                 |
                 v
             [Notif WebSocket FAILED] -> [Error End]
```

## Verifikasi BPMN Error Boundary

Proyek ini menyediakan mekanisme simulasi kegagalan **tanpa mengubah kode**, cukup satu flag di konfigurasi.

### Simulasi Kegagalan PDF (`PDF_GENERATION_FAILED`)
1. Set `app.pdf.simulate-failure: true` di `application.yml`
2. Restart aplikasi
3. Kirim `POST /api/ideb/scrape` dengan NIK valid (misal: `3174012501900001`)
4. **Expected:** WebSocket menerima `[FAILED] PDF_GENERATION_FAILED`, record tersimpan di `ideb_failure_logs`, dan Flowable mengakhiri proses di `errorEndEvent`

```bash
# Kirim request via curl
curl -X POST http://localhost:8080/api/ideb/scrape \
  -H "Content-Type: application/json" \
  -d '{"nik": "3174012501900001"}'

# Cek log kegagalan yang tersimpan di DB
curl http://localhost:8080/api/ideb/failures

# Verifikasi routing BPMN di database Flowable
# SELECT act_id_, act_name_ FROM act_hi_actinst WHERE proc_inst_id_ = '<id>'
# Expected sequence: generatePdfTask → pdfErrorBoundary → handleErrorTask → errorEndEvent
```

### Simulasi Kegagalan Scraping (`SCRAPING_FAILED`)
Gunakan NIK yang tidak ada di data mock (tanpa perubahan config apapun):
```bash
curl -X POST http://localhost:8080/api/ideb/scrape \
  -H "Content-Type: application/json" \
  -d '{"nik": "9999999999999999"}'
# Expected: WebSocket [FAILED] SCRAPING_FAILED
# Expected sequence BPMN: scrapeDataTask → scrapeErrorBoundary → handleErrorTask → errorEndEvent
```

> Panduan verifikasi lengkap beserta query SQL dan expected output ada di [docs/TESTING.md](docs/TESTING.md) — Skenario 3 dan 3b.

---

## Catatan Teknis

- **In-Memory PDF Buffer**: Rendering PDF menggunakan `ByteArrayOutputStream` untuk menjamin atomisitas (mencegah file korup jika render gagal di tengah) serta memungkinkan penulisan ganda ke database (`bytea`) dan disk secara efisien.
- **Asynchronous Execution & WebSocket**: Endpoint scraping mengembalikan respons `HTTP 202 Accepted` seketika untuk mencegah request timeout pada proses scraping, sementara kemajuan tiap tahap di-broadcast real-time ke client via STOMP WebSocket.
- **BPMN Boundary Error Handling**: Kesalahan fatal seperti kegagalan scraping atau rendering PDF ditangkap oleh Boundary Error Event (`SCRAPING_FAILED` dan `PDF_GENERATION_FAILED`) untuk memastikan pencatatan failure log dan notifikasi client tanpa mematikan proses engine.
- **Type-Safe Dynamic Query**: Pencarian dengan QueryDSL memanfaatkan `BooleanBuilder` modular yang mengabaikan parameter bernilai null secara otomatis tanpa query string concatenation yang rentan SQL injection.

Detail arsitektur lebih lanjut dapat dilihat di [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Testing & Video Demo

- Panduan skenario pengujian, collection Hoppscotch, dan verifikasi SQL tersedia di [docs/TESTING.md](file:///d:/Projects/slik-ideb-service/docs/TESTING.md).
- Panduan rekaman dan checklist **Video Demo** pengujian aplikasi tersedia di [docs/screenshots/README.md](file:///d:/Projects/slik-ideb-service/docs/screenshots/README.md).

