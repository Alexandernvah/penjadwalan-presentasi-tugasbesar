-- ==============================================================================
-- Seed Data: db_penjadwalan (Idempotent)
-- Sistem Penjadwalan Presentasi Tugas Besar - Teknik Informatika UNPAR
-- ==============================================================================

-- 1. Akun Pengguna (DOSEN, ADMIN, MAHASISWA)
INSERT INTO akun (email, password, role, aktif)
VALUES
    ('budi.santoso@unpar.ac.id', 'password123', 'DOSEN', TRUE),
    ('widodo.pratama@unpar.ac.id', 'password123', 'DOSEN', TRUE),
    ('yuni.kartika@unpar.ac.id', 'password123', 'DOSEN', TRUE),
    ('admin@unpar.ac.id', 'password123', 'ADMIN', TRUE),
    ('6182201001@student.unpar.ac.id', 'password123', 'MAHASISWA', TRUE),
    ('6182201002@student.unpar.ac.id', 'password123', 'MAHASISWA', TRUE),
    ('6182201003@student.unpar.ac.id', 'password123', 'MAHASISWA', TRUE)
ON CONFLICT (email) DO NOTHING;

-- 2. Profil Dosen
INSERT INTO dosen (nomor_induk_dosen, nama, id_akun)
SELECT '1975031501', 'Dr. Budi Santoso', id_akun
FROM akun
WHERE email = 'budi.santoso@unpar.ac.id'
ON CONFLICT (nomor_induk_dosen) DO NOTHING;

INSERT INTO dosen (nomor_induk_dosen, nama, id_akun)
SELECT '1978042002', 'Ir. Widodo Pratama, M.T.', id_akun
FROM akun
WHERE email = 'widodo.pratama@unpar.ac.id'
ON CONFLICT (nomor_induk_dosen) DO NOTHING;

INSERT INTO dosen (nomor_induk_dosen, nama, id_akun)
SELECT '1982091203', 'Dr. Yuni Kartika, S.T., M.Kom.', id_akun
FROM akun
WHERE email = 'yuni.kartika@unpar.ac.id'
ON CONFLICT (nomor_induk_dosen) DO NOTHING;

-- 3. Profil Mahasiswa
INSERT INTO mahasiswa (npm, nama, id_akun)
SELECT '6182201001', 'Dimas Pratama', id_akun
FROM akun
WHERE email = '6182201001@student.unpar.ac.id'
ON CONFLICT (npm) DO NOTHING;

INSERT INTO mahasiswa (npm, nama, id_akun)
SELECT '6182201002', 'Evelyn Wijaya', id_akun
FROM akun
WHERE email = '6182201002@student.unpar.ac.id'
ON CONFLICT (npm) DO NOTHING;

INSERT INTO mahasiswa (npm, nama, id_akun)
SELECT '6182201003', 'Farhan Akbar', id_akun
FROM akun
WHERE email = '6182201003@student.unpar.ac.id'
ON CONFLICT (npm) DO NOTHING;

-- 4. Master Mata Kuliah & Mata Kuliah Buka (Semester Ganjil 2026/2027 -> kode '26271')
INSERT INTO mata_kuliah (id_matkul, nama, sks)
VALUES
    ('IF2110', 'Algoritma dan Struktur Data', 4),
    ('IF3130', 'Jaringan Komputer', 3),
    ('IF2210', 'Pemrograman Berorientasi Objek', 4),
    ('IF3250', 'Rekayasa Perangkat Lunak', 3)
ON CONFLICT (id_matkul) DO NOTHING;

INSERT INTO mata_kuliah_buka (id_matkul, semester_akademik, nomor_induk_koordinator)
VALUES
    ('IF2110', '26271', '1975031501'),
    ('IF3130', '26271', '1975031501'),
    ('IF2210', '26271', '1978042002'),
    ('IF3250', '26271', '1982091203')
ON CONFLICT (id_matkul, semester_akademik) DO NOTHING;

-- 5. Kelas Kuliah
INSERT INTO kelas (id_matkul_buka, kode_kelas, nomor_induk_penanggung_jawab)
SELECT mkb.id_matkul_buka, 'A', '1975031501'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF2110' AND mkb.semester_akademik = '26271'
ON CONFLICT (id_matkul_buka, kode_kelas) DO NOTHING;

INSERT INTO kelas (id_matkul_buka, kode_kelas, nomor_induk_penanggung_jawab)
SELECT mkb.id_matkul_buka, 'B', '1975031501'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF3130' AND mkb.semester_akademik = '26271'
ON CONFLICT (id_matkul_buka, kode_kelas) DO NOTHING;

INSERT INTO kelas (id_matkul_buka, kode_kelas, nomor_induk_penanggung_jawab)
SELECT mkb.id_matkul_buka, 'A', '1978042002'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF2210' AND mkb.semester_akademik = '26271'
ON CONFLICT (id_matkul_buka, kode_kelas) DO NOTHING;

INSERT INTO kelas (id_matkul_buka, kode_kelas, nomor_induk_penanggung_jawab)
SELECT mkb.id_matkul_buka, 'A', '1982091203'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF3250' AND mkb.semester_akademik = '26271'
ON CONFLICT (id_matkul_buka, kode_kelas) DO NOTHING;

-- 6. Jadwal Kuliah Rutin (Dr. Budi Santoso)
INSERT INTO kuliah (id_kelas, nomor_induk_pengajar, hari, jam_mulai, jam_selesai, ruangan, tipe_pertemuan)
SELECT k.id_kelas, '1975031501', v.hari, v.jam_mulai, v.jam_selesai, v.ruangan, 'KULIAH'
FROM kelas k
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
CROSS JOIN (
    VALUES
        (2::SMALLINT, TIME '08:00', TIME '10:00', 'Lab Komputer 1'),
        (4::SMALLINT, TIME '08:00', TIME '10:00', 'Lab Komputer 1')
) AS v(hari, jam_mulai, jam_selesai, ruangan)
WHERE mkb.id_matkul = 'IF2110'
  AND mkb.semester_akademik = '26271'
  AND k.kode_kelas = 'A'
  AND NOT EXISTS (
      SELECT 1 FROM kuliah kul
      WHERE kul.id_kelas = k.id_kelas
        AND kul.nomor_induk_pengajar = '1975031501'
        AND kul.hari = v.hari
        AND kul.jam_mulai = v.jam_mulai
  );

INSERT INTO kuliah (id_kelas, nomor_induk_pengajar, hari, jam_mulai, jam_selesai, ruangan, tipe_pertemuan)
SELECT k.id_kelas, '1975031501', v.hari, v.jam_mulai, v.jam_selesai, v.ruangan, 'KULIAH'
FROM kelas k
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
CROSS JOIN (
    VALUES
        (1::SMALLINT, TIME '13:00', TIME '15:00', 'Ruang 9013'),
        (3::SMALLINT, TIME '13:00', TIME '15:00', 'Ruang 9013')
) AS v(hari, jam_mulai, jam_selesai, ruangan)
WHERE mkb.id_matkul = 'IF3130'
  AND mkb.semester_akademik = '26271'
  AND k.kode_kelas = 'B'
  AND NOT EXISTS (
      SELECT 1 FROM kuliah kul
      WHERE kul.id_kelas = k.id_kelas
        AND kul.nomor_induk_pengajar = '1975031501'
        AND kul.hari = v.hari
        AND kul.jam_mulai = v.jam_mulai
  );

-- 7. Jadwal Ujian & Pengawas Ujian
INSERT INTO ujian (id_matkul_buka, jenis_ujian, tanggal, jam_mulai, jam_selesai, ruangan)
SELECT mkb.id_matkul_buka, 'UTS', DATE '2026-10-19', TIME '08:00', TIME '10:00', 'Ruang 9012'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF2110' AND mkb.semester_akademik = '26271'
  AND NOT EXISTS (
      SELECT 1 FROM ujian u
      WHERE u.id_matkul_buka = mkb.id_matkul_buka AND u.jenis_ujian = 'UTS'
  );

INSERT INTO ujian (id_matkul_buka, jenis_ujian, tanggal, jam_mulai, jam_selesai, ruangan)
SELECT mkb.id_matkul_buka, 'UAS', DATE '2026-12-09', TIME '10:00', TIME '12:00', 'Ruang 9013'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF3130' AND mkb.semester_akademik = '26271'
  AND NOT EXISTS (
      SELECT 1 FROM ujian u
      WHERE u.id_matkul_buka = mkb.id_matkul_buka AND u.jenis_ujian = 'UAS'
  );

-- Ujian Lab Pemrograman dengan 2 Pengawas (Pak Widodo & Bu Yuni)
INSERT INTO ujian (id_matkul_buka, jenis_ujian, tanggal, jam_mulai, jam_selesai, ruangan)
SELECT mkb.id_matkul_buka, 'UAS', DATE '2026-12-10', TIME '13:00', TIME '15:00', 'Lab Komputer 1'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF2110' AND mkb.semester_akademik = '26271'
  AND NOT EXISTS (
      SELECT 1 FROM ujian u
      WHERE u.id_matkul_buka = mkb.id_matkul_buka AND u.jenis_ujian = 'UAS'
  );

-- Ujian yang diawasi Pak Widodo (untuk skenario Tukar Jadwal)
INSERT INTO ujian (id_matkul_buka, jenis_ujian, tanggal, jam_mulai, jam_selesai, ruangan)
SELECT mkb.id_matkul_buka, 'UTS', DATE '2026-10-21', TIME '08:00', TIME '10:00', 'Ruang 9013'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF3130' AND mkb.semester_akademik = '26271'
  AND NOT EXISTS (
      SELECT 1 FROM ujian u
      WHERE u.id_matkul_buka = mkb.id_matkul_buka AND u.jenis_ujian = 'UTS'
  );

-- Ujian yang sengaja bentrok dengan jadwal mengajar Dr. Budi Santoso (Selasa 08:00 - 10:00)
INSERT INTO ujian (id_matkul_buka, jenis_ujian, tanggal, jam_mulai, jam_selesai, ruangan)
SELECT mkb.id_matkul_buka, 'UTS', DATE '2026-10-20', TIME '08:00', TIME '10:00', 'Lab Komputer 2'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF2210' AND mkb.semester_akademik = '26271'
  AND NOT EXISTS (
      SELECT 1 FROM ujian u
      WHERE u.id_matkul_buka = mkb.id_matkul_buka AND u.jenis_ujian = 'UTS'
  );

-- Ujian tanpa pengawas (0 Pengawas) dan tidak bentrok
INSERT INTO ujian (id_matkul_buka, jenis_ujian, tanggal, jam_mulai, jam_selesai, ruangan)
SELECT mkb.id_matkul_buka, 'UAS', DATE '2026-12-11', TIME '08:00', TIME '10:00', 'Ruang 9122'
FROM mata_kuliah_buka mkb
WHERE mkb.id_matkul = 'IF3250' AND mkb.semester_akademik = '26271'
  AND NOT EXISTS (
      SELECT 1 FROM ujian u
      WHERE u.id_matkul_buka = mkb.id_matkul_buka AND u.jenis_ujian = 'UAS'
  );

-- Penugasan Pengawas Dr. Budi Santoso (UTS IF2110 & UAS IF3130)
INSERT INTO pengawas_ujian (id_ujian, nomor_induk_pengawas)
SELECT u.id_ujian, '1975031501'
FROM ujian u
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = u.id_matkul_buka
WHERE mkb.semester_akademik = '26271'
  AND (
      (mkb.id_matkul = 'IF2110' AND u.jenis_ujian = 'UTS')
      OR (mkb.id_matkul = 'IF3130' AND u.jenis_ujian = 'UAS')
  )
ON CONFLICT (id_ujian, nomor_induk_pengawas) DO NOTHING;

-- Penugasan Multi-Pengawas Lab (UAS IF2110: Pak Widodo & Bu Yuni)
INSERT INTO pengawas_ujian (id_ujian, nomor_induk_pengawas)
SELECT u.id_ujian, d.nidn
FROM ujian u
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = u.id_matkul_buka
CROSS JOIN (VALUES ('1978042002'), ('1982091203')) AS d(nidn)
WHERE mkb.semester_akademik = '26271'
  AND mkb.id_matkul = 'IF2110'
  AND u.jenis_ujian = 'UAS'
ON CONFLICT (id_ujian, nomor_induk_pengawas) DO NOTHING;

-- Penugasan Pak Widodo pada UTS IF3130 (untuk skenario Tukar Jadwal)
INSERT INTO pengawas_ujian (id_ujian, nomor_induk_pengawas)
SELECT u.id_ujian, '1978042002'
FROM ujian u
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = u.id_matkul_buka
WHERE mkb.semester_akademik = '26271'
  AND mkb.id_matkul = 'IF3130'
  AND u.jenis_ujian = 'UTS'
ON CONFLICT (id_ujian, nomor_induk_pengawas) DO NOTHING;

-- 8. Peserta Kelas
INSERT INTO peserta_kelas (npm, id_kelas)
SELECT m.npm, k.id_kelas
FROM mahasiswa m
CROSS JOIN kelas k
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
WHERE m.npm IN ('6182201001', '6182201002', '6182201003')
  AND mkb.semester_akademik = '26271'
  AND mkb.id_matkul IN ('IF2110', 'IF3130')
ON CONFLICT (npm, id_kelas) DO NOTHING;

-- 9. Kelompok Tugas Besar
INSERT INTO kelompok (id_kelas, no_kelompok, kapasitas, topik)
SELECT k.id_kelas, v.no_kelompok, v.kapasitas, v.topik
FROM kelas k
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
CROSS JOIN (
    VALUES
        (1::SMALLINT, 4::SMALLINT, 'Visualisasi Struktur Data Pohon Berimbang'),
        (2::SMALLINT, 4::SMALLINT, 'Simulasi Algoritma Pencarian Jalur Terpendek')
) AS v(no_kelompok, kapasitas, topik)
WHERE mkb.id_matkul = 'IF2110'
  AND mkb.semester_akademik = '26271'
  AND k.kode_kelas = 'A'
ON CONFLICT (id_kelas, no_kelompok) DO NOTHING;

INSERT INTO kelompok (id_kelas, no_kelompok, kapasitas, topik)
SELECT k.id_kelas, 1::SMALLINT, 4::SMALLINT, 'Perancangan Topologi Jaringan Kampus'
FROM kelas k
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
WHERE mkb.id_matkul = 'IF3130'
  AND mkb.semester_akademik = '26271'
  AND k.kode_kelas = 'B'
ON CONFLICT (id_kelas, no_kelompok) DO NOTHING;

-- 10. Anggota Kelompok
INSERT INTO anggota_kelompok (npm, id_kelompok, id_kelas)
SELECT m.npm, kel.id_kelompok, k.id_kelas
FROM mahasiswa m
JOIN kelas k ON TRUE
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
JOIN kelompok kel ON kel.id_kelas = k.id_kelas AND kel.no_kelompok = 1
WHERE mkb.id_matkul = 'IF2110'
  AND mkb.semester_akademik = '26271'
  AND k.kode_kelas = 'A'
  AND m.npm IN ('6182201001', '6182201002', '6182201003')
ON CONFLICT (npm, id_kelas) DO NOTHING;

INSERT INTO anggota_kelompok (npm, id_kelompok, id_kelas)
SELECT m.npm, kel.id_kelompok, k.id_kelas
FROM mahasiswa m
JOIN kelas k ON TRUE
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
JOIN kelompok kel ON kel.id_kelas = k.id_kelas AND kel.no_kelompok = 1
WHERE mkb.id_matkul = 'IF3130'
  AND mkb.semester_akademik = '26271'
  AND k.kode_kelas = 'B'
  AND m.npm IN ('6182201001', '6182201002')
ON CONFLICT (npm, id_kelas) DO NOTHING;

-- 11. Jadwal Presentasi Tugas Besar
INSERT INTO presentasi (id_kelompok, tanggal, jam_mulai, jam_selesai, ruangan, ada_bentrok, catatan)
SELECT kel.id_kelompok, DATE '2026-10-07', TIME '10:00', TIME '10:30', 'Lab Komputer 1', FALSE, 'Presentasi kelompok IF2110'
FROM kelompok kel
JOIN kelas k ON k.id_kelas = kel.id_kelas
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
WHERE mkb.id_matkul = 'IF2110'
  AND mkb.semester_akademik = '26271'
  AND k.kode_kelas = 'A'
  AND kel.no_kelompok = 1
ON CONFLICT (id_kelompok) DO NOTHING;

INSERT INTO presentasi (id_kelompok, tanggal, jam_mulai, jam_selesai, ruangan, ada_bentrok, catatan)
SELECT kel.id_kelompok, DATE '2026-10-09', TIME '09:00', TIME '09:30', 'Ruang 9122', FALSE, 'Presentasi kelompok IF3130'
FROM kelompok kel
JOIN kelas k ON k.id_kelas = kel.id_kelas
JOIN mata_kuliah_buka mkb ON mkb.id_matkul_buka = k.id_matkul_buka
WHERE mkb.id_matkul = 'IF3130'
  AND mkb.semester_akademik = '26271'
  AND k.kode_kelas = 'B'
  AND kel.no_kelompok = 1
ON CONFLICT (id_kelompok) DO NOTHING;
