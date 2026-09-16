# Folder Dokumentasi Visual

Bukti fungsionalitas aplikasi disajikan dalam bentuk **video demo** yang mencakup
seluruh alur pengujian secara end-to-end.

## Video Demo

> 🎬 **Link video demo:** [**Tonton Video Demo**](https://drive.google.com/file/d/1Oldd-ccqldu3XekvwKdHVjGyv4KFG1qq/view?usp=drivesdk)

### Isi yang Ditunjukkan dalam Video

| # | Bagian | Yang Didemonstrasikan |
|---|--------|-----------------------|
| 1 | Halaman Mock SLIK | Tampilan `/mock/slik-data` sebagai target scraping Playwright |
| 2 | WebSocket Connect | Koneksi ke `ws-tester.html`, status "Terhubung" |
| 3 | POST `/api/ideb/scrape` | Response HTTP 202 langsung (non-blocking) |
| 4 | Real-time WebSocket | Event berurutan: `SCRAPING → VALIDATING → GENERATING_PDF → SAVING → SUCCESS` |
| 5 | Download PDF | Buka file PDF SLIK hasil generate |
| 6 | Error Boundary — Scraping | NIK tidak valid → `[FAILED] SCRAPING_FAILED` di WebSocket |
| 7 | Tabel Flowable di DB | Query `act_hi_procinst` menunjukkan `end_activity_id_ = errorEndEvent` |
| 8 | GET `/api/ideb/search` | Filter dinamis `nasabahName`, `statusKredit`, `startDate`, `endDate` |


