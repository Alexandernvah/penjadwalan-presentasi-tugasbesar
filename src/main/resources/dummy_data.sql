-- ==============================================================================
-- Dummy & Seed Data: db_penjadwalan
-- Sistem Penjadwalan Presentasi Tugas Besar - Teknik Informatika UNPAR
-- Jalankan SETELAH master.sql dieksekusi pada PostgreSQL.
-- ==============================================================================

-- 1. Akun Pengguna (ADMIN, DOSEN, MAHASISWA)
INSERT INTO akun (id_akun, email, password, role, aktif) VALUES
(1,  'admin@unpar.ac.id',               'password123', 'ADMIN',     TRUE),
(2,  'budi.santoso@unpar.ac.id',        'password123', 'DOSEN',     TRUE),
(3,  'widodo.pratama@unpar.ac.id',      'password123', 'DOSEN',     TRUE),
(4,  'yuni.kartika@unpar.ac.id',        'password123', 'DOSEN',     TRUE),
(5,  'siti.rahmawati@unpar.ac.id',      'password123', 'DOSEN',     TRUE),
(6,  'andi.wijaya@unpar.ac.id',         'password123', 'DOSEN',     TRUE),
(7,  '6182201001@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE),
(8,  '6182201002@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE),
(9,  '6182201003@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE),
(10, '6182301001@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE),
(11, '6182301002@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE),
(12, '6182301003@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE),
(13, '6182301004@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE),
(14, '6182301005@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE),
(15, '6182301006@student.unpar.ac.id',  'password123', 'MAHASISWA', TRUE);

-- 2. Profil Dosen
INSERT INTO dosen (nomor_induk_dosen, nama, id_akun) VALUES
('1975031501', 'Dr. Budi Santoso',                2),
('1978042002', 'Ir. Widodo Pratama, M.T.',        3),
('1982091203', 'Dr. Yuni Kartika, S.T., M.Kom.',  4),
('1980061202', 'Dr. Siti Rahmawati',              5),
('1983092303', 'Andi Wijaya, M.T.',               6);

-- 3. Profil Mahasiswa
INSERT INTO mahasiswa (npm, nama, id_akun) VALUES
('6182201001', 'Dimas Pratama',   7),
('6182201002', 'Evelyn Wijaya',   8),
('6182201003', 'Farhan Akbar',    9),
('6182301001', 'Rina Maharani',  10),
('6182301002', 'Dimas Pratama',  11),
('6182301003', 'Salsa Aulia',    12),
('6182301004', 'Bagas Nugroho',  13),
('6182301005', 'Citra Lestari',  14),
('6182301006', 'Fajar Ramadhan', 15);

-- 4. Master Mata Kuliah
INSERT INTO mata_kuliah (id_matkul, nama, sks) VALUES
('IF2110', 'Algoritma dan Struktur Data',    4),
('IF3130', 'Jaringan Komputer',              3),
('IF2210', 'Pemrograman Berorientasi Objek', 4),
('IF2250', 'Rekayasa Perangkat Lunak',       3),
('IF3250', 'Rekayasa Perangkat Lunak',       3);

-- 5. Mata Kuliah Dibuka (semester aktif 26271 & semester sebelumnya 25261)
INSERT INTO mata_kuliah_buka (id_matkul_buka, id_matkul, semester_akademik, nomor_induk_koordinator) VALUES
(1, 'IF2110', '26271', '1975031501'),
(2, 'IF3130', '26271', '1975031501'),
(3, 'IF2210', '26271', '1978042002'),
(4, 'IF3250', '26271', '1982091203'),
(5, 'IF2110', '25261', '1975031501'),
(6, 'IF3130', '25261', '1975031501'),
(7, 'IF2250', '25261', '1980061202');

-- 6. Kelas Kuliah (id 101 & 102 cocok dengan default/fallback dashboard & penjadwalan)
INSERT INTO kelas (id_kelas, id_matkul_buka, kode_kelas, nomor_induk_penanggung_jawab) VALUES
(101, 1, 'A', '1975031501'),
(102, 2, 'B', '1975031501'),
(103, 3, 'A', '1978042002'),
(104, 4, 'A', '1982091203'),
(201, 5, 'A', '1975031501'),
(202, 6, 'B', '1975031501'),
(203, 7, 'A', '1980061202');

-- 7. Jadwal Kuliah Rutin (hari: 1 = Senin ... 7 = Minggu)
INSERT INTO kuliah (id_kelas, nomor_induk_pengajar, hari, jam_mulai, jam_selesai, ruangan, tipe_pertemuan) VALUES
(101, '1975031501', 2, TIME '08:00', TIME '10:00', 'Lab Komputer 1', 'KULIAH'),
(101, '1975031501', 4, TIME '08:00', TIME '10:00', 'Lab Komputer 1', 'KULIAH'),
(102, '1975031501', 1, TIME '13:00', TIME '15:00', 'Ruang 9013',     'KULIAH'),
(102, '1975031501', 3, TIME '13:00', TIME '15:00', 'Ruang 9013',     'KULIAH'),
(201, '1975031501', 1, TIME '08:00', TIME '10:30', 'Lab Komputer 1', 'KULIAH'),
(202, '1975031501', 3, TIME '13:00', TIME '15:30', 'Ruang 9122',     'KULIAH'),
(203, '1980061202', 2, TIME '10:00', TIME '12:30', 'Ruang 9010',     'KULIAH');

-- 8. Jadwal Ujian (1..6 semester 26271, 7..10 semester 25261)
INSERT INTO ujian (id_ujian, id_matkul_buka, jenis_ujian, tanggal, jam_mulai, jam_selesai, ruangan) VALUES
(1,  1, 'UTS', DATE '2026-10-19', TIME '08:00', TIME '10:00', 'Ruang 9012'),
(2,  2, 'UAS', DATE '2026-12-09', TIME '10:00', TIME '12:00', 'Ruang 9013'),
(3,  1, 'UAS', DATE '2026-12-10', TIME '13:00', TIME '15:00', 'Lab Komputer 1'),
(4,  2, 'UTS', DATE '2026-10-21', TIME '08:00', TIME '10:00', 'Ruang 9013'),
(5,  3, 'UTS', DATE '2026-10-20', TIME '08:00', TIME '10:00', 'Lab Komputer 2'),
(6,  4, 'UAS', DATE '2026-12-11', TIME '08:00', TIME '10:00', 'Ruang 9122'),
(7,  5, 'UTS', DATE '2026-10-12', TIME '08:00', TIME '10:00', 'Ruang 9010'),
(8,  5, 'UAS', DATE '2026-12-14', TIME '08:00', TIME '10:00', 'Ruang 9010'),
(9,  6, 'UTS', DATE '2026-10-13', TIME '13:00', TIME '15:00', 'Ruang 9122'),
(10, 7, 'UAS', DATE '2026-12-15', TIME '10:00', TIME '12:00', 'Ruang 9011');

-- 9. Pengawas Ujian (mencakup skenario multi-pengawas, tukar jadwal, bentrok mengajar, & 0 pengawas)
INSERT INTO pengawas_ujian (id_ujian, nomor_induk_pengawas) VALUES
(1,  '1975031501'),
(2,  '1975031501'),
(3,  '1978042002'),
(3,  '1982091203'),
(4,  '1978042002'),
(7,  '1980061202'),
(8,  '1983092303'),
(9,  '1983092303'),
(10, '1975031501');

-- 10. Peserta Kelas
INSERT INTO peserta_kelas (npm, id_kelas) VALUES
('6182201001', 101),
('6182201002', 101),
('6182201003', 101),
('6182201001', 102),
('6182201002', 102),
('6182201003', 102),
('6182301001', 201),
('6182301002', 201),
('6182301003', 201),
('6182301004', 201),
('6182301005', 202),
('6182301006', 202),
('6182301001', 202),
('6182301002', 202);

-- 11. Kelompok Tugas Besar (durasi_presentasi diisi eksplisit)
INSERT INTO kelompok (id_kelompok, id_kelas, no_kelompok, kapasitas, durasi_presentasi, topik) VALUES
(1, 101, 1, 4, 30, 'Visualisasi Struktur Data Pohon Berimbang'),
(2, 101, 2, 4, 30, 'Simulasi Algoritma Pencarian Jalur Terpendek'),
(3, 102, 1, 4, 30, 'Perancangan Topologi Jaringan Kampus'),
(4, 201, 1, 3, 30, 'Sistem Rekomendasi Buku'),
(5, 201, 2, 3, 30, 'Aplikasi Penjadwalan Presentasi'),
(6, 202, 1, 2, 30, 'Simulasi Routing OSPF');

-- 12. Anggota Kelompok (npm harus terdaftar sebagai peserta di kelas yang sama)
INSERT INTO anggota_kelompok (npm, id_kelompok, id_kelas) VALUES
('6182201001', 1, 101),
('6182201002', 1, 101),
('6182201003', 1, 101),
('6182201001', 3, 102),
('6182201002', 3, 102),
('6182301001', 4, 201),
('6182301002', 4, 201),
('6182301003', 5, 201),
('6182301004', 5, 201),
('6182301005', 6, 202),
('6182301006', 6, 202);

-- 13. Jadwal Presentasi Tugas Besar (kelompok 6 ditandai bentrok sebagai contoh pengujian)
INSERT INTO presentasi (id_kelompok, tanggal, jam_mulai, jam_selesai, ruangan, ada_bentrok, catatan) VALUES
(1, DATE '2026-10-07', TIME '10:00', TIME '10:30', 'Lab Komputer 1', FALSE, 'Presentasi kelompok IF2110'),
(3, DATE '2026-10-09', TIME '09:00', TIME '09:30', 'Ruang 9122',     FALSE, 'Presentasi kelompok IF3130'),
(4, DATE '2026-12-01', TIME '09:00', TIME '09:30', 'Ruang 9010',     FALSE, NULL),
(5, DATE '2026-12-01', TIME '09:30', TIME '10:00', 'Ruang 9010',     FALSE, NULL),
(6, DATE '2026-12-02', TIME '14:00', TIME '14:30', 'Ruang 9122',     TRUE,  'Contoh data bentrok untuk pengujian');

-- 14. Sinkronkan seluruh SERIAL sequence agar INSERT berikutnya dari Spring Boot tidak bentrok
SELECT setval(pg_get_serial_sequence('akun', 'id_akun'),                       COALESCE((SELECT MAX(id_akun) FROM akun), 1));
SELECT setval(pg_get_serial_sequence('mata_kuliah_buka', 'id_matkul_buka'),    COALESCE((SELECT MAX(id_matkul_buka) FROM mata_kuliah_buka), 1));
SELECT setval(pg_get_serial_sequence('kelas', 'id_kelas'),                     COALESCE((SELECT MAX(id_kelas) FROM kelas), 1));
SELECT setval(pg_get_serial_sequence('kuliah', 'id_kuliah'),                   COALESCE((SELECT MAX(id_kuliah) FROM kuliah), 1));
SELECT setval(pg_get_serial_sequence('ujian', 'id_ujian'),                     COALESCE((SELECT MAX(id_ujian) FROM ujian), 1));
SELECT setval(pg_get_serial_sequence('peserta_kelas', 'id_peserta'),           COALESCE((SELECT MAX(id_peserta) FROM peserta_kelas), 1));
SELECT setval(pg_get_serial_sequence('kelompok', 'id_kelompok'),               COALESCE((SELECT MAX(id_kelompok) FROM kelompok), 1));
SELECT setval(pg_get_serial_sequence('anggota_kelompok', 'id_anggota'),        COALESCE((SELECT MAX(id_anggota) FROM anggota_kelompok), 1));
SELECT setval(pg_get_serial_sequence('presentasi', 'id_presentasi'),           COALESCE((SELECT MAX(id_presentasi) FROM presentasi), 1));
