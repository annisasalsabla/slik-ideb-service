# Template Email Pengiriman Tugas Technical Test

Gunakan template di bawah ini untuk mengirimkan hasil technical test ke tim HR & Tech Lead.

---

### **Header Email**
- **To:** `jamiko@skyworx.co.id`
- **CC:** `recruitment@skyworx.co.id`
- **Subject:** `Test Backend Developer - EPSRND Padang - Annisa Salsabila`

---

### **Isi Email (Body Email)**

```text
Yth. Tim Recruiter & Tech Lead PT Skyworx Indonesia,

Perkenalkan, saya Annisa Salsabila. Terima kasih atas kesempatan yang diberikan untuk mengikuti technical test posisi Backend Developer (EPSRND Padang). 

Berikut saya lampirkan hasil pengerjaan prototype microservice "Generate SLIK Report (IDEB)" sesuai dengan spesifikasi yang diberikan:

1. Repository GitHub: 
   https://github.com/annisasalsabla/slik-ideb-service
   (Catatan: Jika repo diset private, akses telah/akan di-invite ke jamiko@skyworx.co.id)

2. Video Demo Singkat (Loom / Google Drive): 
   [PASTE LINK VIDEO DEMO 2-4 MENIT DI SINI]

3. File Cadangan (ZIP): 
   Terlampir / Link Google Drive: [PASTE LINK DRIVE JIKA TIDAK DILAMPIRKAN LANGSUNG]

---
Ringkasan Implementasi:
1. Web Scraping: Playwright headless Chromium dengan try-with-resources dan selector dinamis pada data mock portal SLIK.
2. Workflow Engine: Flowable 7.x (BPMN 2.0) lengkap dengan Service Tasks bertahap dan Boundary Error Events (SCRAPING_FAILED & PDF_GENERATION_FAILED) yang mengarah ke pencatatan failure log.
3. Real-time Notification: Spring WebSocket + STOMP (dengan SockJS dan endpoint non-SockJS), broadcast per-requestId dan channel global. Tersedia antarmuka tester di /ws-tester.html.
4. PDF Generation & Memory Management: OpenPDF Flying Saucer + Thymeleaf dengan ByteArrayOutputStream in-memory buffer sebelum disimpan ke DB (bytea) dan file system.
5. Dynamic Query: QueryDSL 5.1.0 (classifier jakarta) dengan BooleanBuilder modular dan pagination pada endpoint GET /api/ideb/search.
6. Testing & Dokumentasi: Dilengkapi 9 unit test (JUnit 5 + Mockito), Hoppscotch Collection (docs/hoppscotch-collection.json), DDL SQL (docs/database-setup.sql), dan panduan lengkap di README.md & docs/TESTING.md.

Cara Cepat Menjalankan:
- Setup DB: PostgreSQL lokal 'slik_ideb_db' (skrip di docs/database-setup.sql)
- Install Browser: mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
- Jalankan Service: mvn spring-boot:run
- Demo Realtime: Buka browser di http://localhost:8080/ws-tester.html

Demikian hasil pengerjaan technical test ini saya sampaikan. Besar harapan saya untuk dapat berdiskusi lebih lanjut pada tahap wawancara teknikal. Atas perhatian dan kesempatannya, saya ucapkan terima kasih.

Hormat saya,
Annisa Salsabila
Telp/WA: [Nomor Telepon Anda]
LinkedIn: [Link LinkedIn Anda]
GitHub: https://github.com/annisasalsabla
```
