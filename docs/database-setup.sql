-- ========================================================
-- DATABASE SETUP SQL - SLIK IDEB SERVICE
-- Skyworx Co. ID
-- ========================================================
-- Jalankan script ini di TablePlus atau psql sebelum
-- menjalankan aplikasi pertama kali.
-- ========================================================

-- 1. Buat database (jalankan sebagai superuser postgres)
CREATE DATABASE slik_ideb_db;

-- 2. Connect ke slik_ideb_db, lalu jalankan sisanya

-- ========================================================
-- DDL TABEL UTAMA (sebagai referensi - JPA akan auto-create)
-- ========================================================

CREATE TABLE IF NOT EXISTS ideb_reports (
    id                  BIGSERIAL PRIMARY KEY,
    request_id          VARCHAR(36) UNIQUE NOT NULL,
    nik                 VARCHAR(16),
    nasabah_name        VARCHAR(200),
    status_kredit       VARCHAR(50),
    nominal_tagihan     NUMERIC(15, 2),
    nama_bank           VARCHAR(200),
    kolektibilitas      VARCHAR(10),
    raw_json            TEXT,
    pdf_path            VARCHAR(500),
    pdf_content         BYTEA,                      -- Konten PDF disimpan sebagai binary
    process_instance_id VARCHAR(100),
    status              VARCHAR(20) DEFAULT 'PROCESSING',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index untuk performa query
CREATE INDEX IF NOT EXISTS idx_ideb_nik ON ideb_reports(nik);
CREATE INDEX IF NOT EXISTS idx_ideb_request_id ON ideb_reports(request_id);
CREATE INDEX IF NOT EXISTS idx_ideb_status_kredit ON ideb_reports(status_kredit);
CREATE INDEX IF NOT EXISTS idx_ideb_created_at ON ideb_reports(created_at);
CREATE INDEX IF NOT EXISTS idx_ideb_nasabah_name ON ideb_reports(nasabah_name);

-- ========================================================
-- DDL TABEL LOG KEGAGALAN
-- ========================================================

CREATE TABLE IF NOT EXISTS ideb_failure_logs (
    id                  BIGSERIAL PRIMARY KEY,
    process_instance_id VARCHAR(100) NOT NULL,
    request_id          VARCHAR(36) NOT NULL,
    error_code          VARCHAR(100),
    error_message       TEXT,
    failed_task         VARCHAR(200),
    stacktrace_summary  TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_failure_request_id ON ideb_failure_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_failure_error_code ON ideb_failure_logs(error_code);

-- ========================================================
-- DATA DUMMY UNTUK TESTING ENDPOINT SEARCH
-- (Data dummy ini mencerminkan data yang bisa di-scrape dari halaman mock)
-- ========================================================

INSERT INTO ideb_reports (request_id, nik, nasabah_name, status_kredit, nominal_tagihan,
    nama_bank, kolektibilitas, raw_json, process_instance_id, status, created_at)
VALUES
(
    'dummy-req-001-budi-santoso',
    '3174012501900001',
    'Budi Santoso',
    'LANCAR',
    150000000.00,
    'Bank Mandiri',
    '1',
    '{"nik":"3174012501900001","nasabahName":"Budi Santoso","statusKredit":"LANCAR","namaBank":"Bank Mandiri"}',
    'dummy-proc-001',
    'SUCCESS',
    '2026-09-01 10:00:00'
),
(
    'dummy-req-002-siti-rahayu',
    '3273055506850002',
    'Siti Rahayu',
    'DALAM PERHATIAN KHUSUS',
    85000000.00,
    'Bank BRI',
    '2',
    '{"nik":"3273055506850002","nasabahName":"Siti Rahayu","statusKredit":"DALAM PERHATIAN KHUSUS","namaBank":"Bank BRI"}',
    'dummy-proc-002',
    'SUCCESS',
    '2026-09-05 14:30:00'
),
(
    'dummy-req-003-ahmad-fauzi',
    '3578041204920003',
    'Ahmad Fauzi',
    'MACET',
    320000000.00,
    'Bank BCA',
    '5',
    '{"nik":"3578041204920003","nasabahName":"Ahmad Fauzi","statusKredit":"MACET","namaBank":"Bank BCA"}',
    'dummy-proc-003',
    'SUCCESS',
    '2026-09-10 09:15:00'
),
(
    'dummy-req-004-dewi-failed',
    '3471061507880004',
    'Dewi Kusumawati',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'dummy-proc-004',
    'FAILED',
    '2026-09-12 11:00:00'
),
(
    'dummy-req-005-indah',
    '3374022203950006',
    'Indah Permatasari',
    'LANCAR',
    500000000.00,
    'Bank Danamon',
    '1',
    '{"nik":"3374022203950006","nasabahName":"Indah Permatasari","statusKredit":"LANCAR","namaBank":"Bank Danamon"}',
    'dummy-proc-005',
    'SUCCESS',
    '2026-09-15 08:00:00'
);

-- ========================================================
-- CATATAN TABEL FLOWABLE (otomatis dibuat oleh Flowable 7.x)
-- ========================================================
-- Saat aplikasi pertama kali start dengan flowable.database-schema-update=true,
-- Flowable akan otomatis membuat tabel berikut di slik_ideb_db:
--
-- Tabel Runtime (ACT_RU_*):
--   ACT_RU_EXECUTION     - Process instances yang sedang berjalan
--   ACT_RU_TASK          - User tasks yang aktif
--   ACT_RU_VARIABLE      - Process variables
--   ACT_RU_JOB           - Async jobs yang pending
--   ACT_RU_DEADLETTER_JOB - Jobs yang gagal permanen
--   ACT_RU_TIMER_JOB     - Timer jobs
--
-- Tabel History (ACT_HI_*):
--   ACT_HI_PROCINST      - Riwayat semua process instance (sukses & gagal)
--   ACT_HI_ACTINST       - Riwayat semua activity yang dieksekusi
--   ACT_HI_VARINST       - Riwayat process variables
--   ACT_HI_TASKINST      - Riwayat user tasks
--
-- Tabel Repository (ACT_RE_*):
--   ACT_RE_PROCDEF       - Definisi proses (BPMN yang terdeploy)
--   ACT_RE_DEPLOYMENT    - Info deployment
--
-- Cara cek di TablePlus:
-- 1. Buka koneksi ke slik_ideb_db
-- 2. Di panel kiri, cari tabel yang diawali dengan ACT_
-- 3. Jalankan: SELECT * FROM ACT_RE_PROCDEF; (untuk cek proses yang terdeploy)
-- 4. Jalankan: SELECT * FROM ACT_HI_PROCINST ORDER BY START_TIME_ DESC;
--             (untuk melihat riwayat proses yang sudah selesai)
