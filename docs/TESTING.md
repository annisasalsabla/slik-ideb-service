# TESTING GUIDE - SLIK IDEB Service

## Prasyarat

- Aplikasi berjalan di `http://localhost:8080`
- Database `slik_ideb_db` sudah dibuat dan terhubung
- Playwright browser sudah terinstall (jalankan `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"`)

> **PENTING - Hoppscotch Web:** Untuk memanggil `localhost` dari Hoppscotch versi web
> (app.hoppscotch.io), Anda perlu salah satu dari:
> 1. **Hoppscotch Browser Extension** - Install di Chrome/Firefox, lalu aktifkan di halaman Hoppscotch
> 2. **Hoppscotch Agent** - Download dari https://hoppscotch.io/download, jalankan lokal
> 3. **Hoppscotch Desktop** - Aplikasi desktop yang tidak terbatas CORS
>
> Alternatif: gunakan `curl`, Postman, atau Insomnia yang tidak punya limitasi CORS.

---

## Skenario 1: Happy Path (Scraping Berhasil)

### Langkah 1: Buka WebSocket Tester
Buka browser ke: `http://localhost:8080/ws-tester.html`
- Klik **Hubungkan** untuk connect WebSocket
- Panel log akan menampilkan "Berhasil terhubung"

### Langkah 2: Kirim Request Generate
**Via Hoppscotch:**
```
POST http://localhost:8080/api/ideb/scrape
Content-Type: application/json

{
  "nik": "3174012501900001",
  "nasabahName": "Budi Santoso"
}
```

**Expected Response (HTTP 202):**
```json
{
  "success": true,
  "status": 202,
  "message": "Request diterima, proses berjalan di background",
  "data": {
    "status": "ACCEPTED",
    "requestId": "8b1f...-...",
    "message": "Proses generate SLIK report sedang berjalan. Silakan pantau via WebSocket.",
    "websocketTopic": "/topic/ideb/8b1f...-..."
  }
}
```

### Langkah 3: Pantau WebSocket
Di ws-tester.html, Anda akan melihat notifikasi berturutan:
```
[PROCESSING] step: SCRAPING
[PROCESSING] step: VALIDATING
[PROCESSING] step: GENERATING_PDF
[PROCESSING] step: SAVING
[SUCCESS] PDF SLIK berhasil di-generate
         url: http://localhost:8080/api/ideb/report/{id}/download
```

### Langkah 4: Download PDF
```
GET http://localhost:8080/api/ideb/report/{id}/download
```
Browser akan mendownload file `SLIK_3174012501900001_xxxxxxxx.pdf`

### Langkah 5: Verifikasi di TablePlus
1. Buka TablePlus, connect ke `slik_ideb_db`
2. Query: `SELECT * FROM ideb_reports ORDER BY created_at DESC LIMIT 5;`
3. Pastikan kolom `status = 'SUCCESS'` dan `pdf_content IS NOT NULL`
4. Query Flowable: `SELECT * FROM act_hi_procinst ORDER BY start_time_ DESC LIMIT 5;`

---

## Skenario 2: Gagal Scraping (NIK Tidak Ada)

### Setup
Tidak perlu konfigurasi tambahan. Gunakan NIK yang tidak ada di halaman mock.

### Request
```
POST http://localhost:8080/api/ideb/scrape
Content-Type: application/json

{
  "nik": "9999999999999999"
}
```

### Expected Flow
1. HTTP Response: `202 Accepted` dengan requestId
2. WebSocket event: `[PROCESSING] step: SCRAPING`
3. Playwright membuka halaman mock, tidak menemukan NIK 9999...
4. `ScrapingException` dilempar → `BpmnError("SCRAPING_FAILED")`
5. Boundary Error Event terpicu di BPMN
6. `HandleErrorDelegate` menyimpan ke `ideb_failure_logs`
7. WebSocket event: `[FAILED] errorCode: SCRAPING_FAILED`

### Verifikasi di TablePlus
```sql
-- Cek log kegagalan
SELECT * FROM ideb_failure_logs ORDER BY created_at DESC LIMIT 5;

-- Cek status report
SELECT request_id, status FROM ideb_reports 
WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT 5;

-- Cek history Flowable (proses yang berakhir di Error End Event)
SELECT proc_inst_id_, start_time_, end_time_, end_activity_id_ 
FROM act_hi_procinst ORDER BY start_time_ DESC LIMIT 5;
```

---

## Skenario 3: Verifikasi BPMN Error Boundary — PDF Generation Failed

> **Tujuan:** Membuktikan bahwa `BoundaryErrorEvent` pada task `generatePdfTask` di BPMN
> benar-benar meng-intercept exception, mengalihkan alur ke `handleErrorTask`,
> mencatat log kegagalan di database, dan mengirimkan notifikasi `FAILED` via WebSocket.
> Pengujian ini tidak memerlukan perubahan kode — cukup satu flag konfigurasi.

---

### Step 1: Aktifkan Mode Simulasi Kegagalan

Buka `src/main/resources/application.yml` dan ubah:

```yaml
app:
  pdf:
    simulate-failure: true   # <-- ubah dari false ke true
```

Lalu **restart aplikasi**:
```bash
mvn spring-boot:run
```

Konfirmasi aplikasi siap:
```
GET http://localhost:8080/actuator/health
```
Expected: `{"status": "UP"}`

---

### Step 2: Hubungkan WebSocket Tester

Buka browser ke `http://localhost:8080/ws-tester.html` dan klik **Hubungkan**.
Panel log menampilkan:
```
Berhasil terhubung ke WebSocket
```

---

### Step 3: Kirim Request ke `POST /api/ideb/scrape`

```bash
curl -X POST http://localhost:8080/api/ideb/scrape \
  -H "Content-Type: application/json" \
  -d '{"nik": "3174012501900001", "nasabahName": "Budi Santoso"}'
```

**Expected HTTP Response — 202 Accepted:**
```json
{
  "success": true,
  "status": 202,
  "message": "Request diterima, proses berjalan di background",
  "data": {
    "status": "ACCEPTED",
    "requestId": "<uuid>",
    "websocketTopic": "/topic/ideb/<uuid>"
  }
}
```

> Bukti non-blocking: server langsung merespons `202` tanpa menunggu proses selesai.

---

### Step 4: Amati Urutan Event di WebSocket Tester

Karena scraping & validasi akan berhasil, tetapi PDF akan gagal, urutan event yang terlihat:

```
[PROCESSING]  step: SCRAPING         ← ScrapeDataDelegate berjalan
[PROCESSING]  step: VALIDATING       ← ValidateDataDelegate berjalan
[PROCESSING]  step: GENERATING_PDF   ← GeneratePdfDelegate mulai berjalan
[FAILED]      errorCode: PDF_GENERATION_FAILED
                 message: Process failed: Simulated PDF failure for BPMN Error Boundary testing
```

> Bukti error boundary: proses **tidak berhenti tiba-tiba** (no 500 error ke client),
> melainkan mengalir teratur ke `handleErrorTask` dan mengirim event `FAILED`.

---

### Step 5: Verifikasi Log Aplikasi (Console/Terminal)

Pada log Spring Boot, pastikan rangkaian log berikut muncul secara berurutan:

```log
[BPMN] Executing scrape task: requestId=<uuid>
[BPMN] Scrape task completed: requestId=<uuid>
[BPMN] Executing validate task: requestId=<uuid>
[BPMN] Validation passed: requestId=<uuid>
[BPMN] Executing generate PDF task: requestId=<uuid>
WARN  - Simulated PDF failure triggered (app.pdf.simulate-failure=true)
[BPMN] PDF generation failed: requestId=<uuid>, error=Simulated PDF failure...
[BPMN] Handling process failure: requestId=<uuid>, errorCode=PDF_GENERATION_FAILED, task=Generate PDF
[BPMN] Failure log persisted for requestId=<uuid>
WARN  - Sent failure notification: requestId=<uuid>, errorCode=PDF_GENERATION_FAILED
```

> Bukti alur BPMN: log menunjukkan task `Generate PDF` gagal, lalu `Handling process failure`
> langsung dipanggil — membuktikan Error Boundary Event mengalihkan eksekusi ke `handleErrorTask`.

---

### Step 6: Verifikasi Database (TablePlus / psql)

#### 6a. Cek tabel `ideb_failure_logs` — harus ada record baru:
```sql
SELECT
    id,
    request_id,
    error_code,
    error_message,
    failed_task,
    created_at
FROM ideb_failure_logs
ORDER BY created_at DESC
LIMIT 5;
```

**Expected result:**
```
 id | request_id | error_code            | failed_task  | error_message
----+------------+-----------------------+--------------+------------------------------------------
  1 | <uuid>     | PDF_GENERATION_FAILED | Generate PDF | Simulated PDF failure for BPMN Error...
```

#### 6b. Cek Flowable history — proses berakhir di `errorEndEvent`:
```sql
SELECT
    proc_inst_id_,
    proc_def_key_,
    start_time_,
    end_time_,
    end_activity_id_,
    delete_reason_
FROM act_hi_procinst
ORDER BY start_time_ DESC
LIMIT 5;
```

**Expected result:**
```
 proc_def_key_      | end_activity_id_
-------------------+-----------------
 idebReportProcess | errorEndEvent    ← BPMN routing ke Error End Event
```

#### 6c. Cek activity history — urutan task yang dieksekusi:
```sql
SELECT
    act_id_,
    act_name_,
    act_type_,
    start_time_,
    end_time_
FROM act_hi_actinst
WHERE proc_inst_id_ = '<ganti-dengan-proc_inst_id_-dari-query-di-atas>'
ORDER BY start_time_ ASC;
```

**Expected result** (urutan tasks yang dieksekusi di BPMN):
```
 act_id_             | act_name_                  | act_type_
---------------------+----------------------------+-----------------
 startEvent          | Mulai Proses SLIK          | startEvent
 scrapeDataTask      | Scrape Data Eksternal      | serviceTask
 validateDataTask    | Validasi Data JSON         | serviceTask
 generatePdfTask     | Generate PDF               | serviceTask      ← gagal di sini
 pdfErrorBoundary    | (boundary event)           | boundaryEvent    ← boundary terpicu
 handleErrorTask     | Catat Log Kegagalan        | serviceTask      ← dialihkan ke sini
 errorEndEvent       | Proses Gagal               | endEvent
```

> Bukti alur BPMN: query ini membuktikan secara visual bahwa `pdfErrorBoundary` terpicu
> dan proses mengalir ke `handleErrorTask` bukan ke `saveToDbTask`.

#### 6d. Cek endpoint `/api/ideb/failures`:
```bash
curl http://localhost:8080/api/ideb/failures
```

**Expected:**
```json
{
  "success": true,
  "data": [
    {
      "requestId": "<uuid>",
      "errorCode": "PDF_GENERATION_FAILED",
      "errorMessage": "Simulated PDF failure for BPMN Error Boundary testing",
      "failedTask": "Generate PDF",
      "createdAt": "..."
    }
  ]
}
```

---

### Step 7: Reset Setelah Testing

Kembalikan `application.yml` ke kondisi normal:
```yaml
app:
  pdf:
    simulate-failure: false   # <-- kembalikan ke false
```
Restart aplikasi.

---

## Skenario 3b: Verifikasi BPMN Error Boundary — Scraping Failed

> **Tujuan:** Membuktikan bahwa `BoundaryErrorEvent` pada task `scrapeDataTask` bekerja
> ketika NIK/nama tidak ditemukan di halaman mock SLIK (tidak perlu perubahan config).

### Step 1: Pastikan `simulate-failure: false` (kondisi normal)

### Step 2: Kirim request dengan NIK yang tidak ada di data mock:
```bash
curl -X POST http://localhost:8080/api/ideb/scrape \
  -H "Content-Type: application/json" \
  -d '{"nik": "9999999999999999"}'
```

### Step 3: Amati event di WebSocket Tester:
```
[PROCESSING]  step: SCRAPING
[FAILED]      errorCode: SCRAPING_FAILED
                 message: Process failed: Debtor data not found for NIK=9999999999999999
```

### Step 4: Verifikasi database:
```sql
-- Cek failure log tersimpan
SELECT error_code, error_message, failed_task
FROM ideb_failure_logs
ORDER BY created_at DESC LIMIT 1;
```

**Expected:**
```
 error_code      | failed_task             | error_message
-----------------+-------------------------+-----------------------------------------------
 SCRAPING_FAILED | Scrape Data Eksternal   | Debtor data not found for NIK=9999...
```

```sql
-- Cek Flowable routing ke scrapeErrorBoundary
SELECT act_id_, act_name_
FROM act_hi_actinst
WHERE proc_inst_id_ = '<proc_inst_id>'
ORDER BY start_time_ ASC;
```

**Expected:** Sequence `startEvent → scrapeDataTask → scrapeErrorBoundary → handleErrorTask → errorEndEvent`

---

## Skenario 4: Search dengan QueryDSL

### Request - Filter Ganda
```
GET http://localhost:8080/api/ideb/search?nasabahName=Budi&statusKredit=LANCAR&startDate=2026-01-01&endDate=2026-12-31&page=0&size=10
```

### Request - Search by NIK
```
GET http://localhost:8080/api/ideb/search?nik=3174012501900001
```

### Request - Semua (tanpa filter)
```
GET http://localhost:8080/api/ideb/search
```

### Expected Response
```json
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": N,
    "totalPages": M,
    "page": 0,
    "size": 10
  }
}
```

---

## Skenario 5: WebSocket via Hoppscotch STOMP Tab

> **Catatan:** Jika endpoint SockJS bermasalah dengan Hoppscotch, gunakan `ws-tester.html` (sudah disediakan).

### Cara Pakai Tab Realtime - STOMP di Hoppscotch:
1. Buka Hoppscotch → tab **Realtime** → pilih **STOMP**
2. **URL:** `ws://localhost:8080/ws-ideb` (tanpa SockJS)
3. Klik **Connect**
4. Di bagian **Subscribe**, masukkan topic: `/topic/ideb/notifications`
5. Klik **Subscribe**
6. Buka tab lain, kirim `POST /api/ideb/scrape`
7. Notifikasi akan muncul di panel STOMP Hoppscotch

### Alternative (lebih andal): ws-tester.html
```
http://localhost:8080/ws-tester.html
```
- SockJS client sudah dikonfigurasi
- Panel log realtime tersedia
- Bisa generate report langsung dari halaman ini

---

## Validasi Cepat — Checklist Evaluator

### Fitur Inti
```
[ ] Aplikasi start tanpa error, log menunjukkan "idebReportProcess" terdeploy oleh Flowable
[ ] GET /actuator/health mengembalikan status UP dengan koneksi database aktif
[ ] GET /mock/slik-data menampilkan halaman tabel SLIK (target scraping)
[ ] POST /api/ideb/scrape mengembalikan HTTP 202 Accepted langsung (non-blocking)
[ ] WebSocket tester menerima urutan notifikasi: SCRAPING → VALIDATING → GENERATING_PDF → SAVING → SUCCESS
[ ] File PDF tersimpan di folder pdf-output/ dan dapat didownload via endpoint
[ ] GET /api/ideb/report/{id}/download mendownload file PDF SLIK yang valid
[ ] GET /api/ideb/search berfungsi dengan semua kombinasi filter (nama, NIK, status, tanggal)
[ ] Tabel Flowable (ACT_HI_*, ACT_RE_*, dll) terbuat otomatis di database
```

### BPMN Error Boundary (Wajib Diverifikasi)
```
[ ] Skenario 3  : app.pdf.simulate-failure=true → WebSocket menerima [FAILED] PDF_GENERATION_FAILED
[ ] Skenario 3  : Record tersimpan di tabel ideb_failure_logs dengan error_code = 'PDF_GENERATION_FAILED'
[ ] Skenario 3  : act_hi_actinst menunjukkan urutan task berakhir di pdfErrorBoundary → handleErrorTask → errorEndEvent
[ ] Skenario 3b : NIK tidak valid → WebSocket menerima [FAILED] SCRAPING_FAILED
[ ] Skenario 3b : Record tersimpan di ideb_failure_logs dengan error_code = 'SCRAPING_FAILED'
[ ] Skenario 3b : act_hi_actinst menunjukkan urutan task berakhir di scrapeErrorBoundary → handleErrorTask → errorEndEvent
[ ] GET /api/ideb/failures mengembalikan daftar semua log kegagalan yang tersimpan
```

### Setelah Pengujian Error Boundary Selesai
```
[ ] app.pdf.simulate-failure dikembalikan ke false
[ ] Happy path (Skenario 1) dijalankan ulang untuk memastikan sistem kembali normal
```
