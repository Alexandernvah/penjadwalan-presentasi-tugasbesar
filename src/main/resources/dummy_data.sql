-- ==============================================================================
-- Dummy data: db_penjadwalan
-- Jalankan SETELAH skema (13 tabel) dibuat. Semua data fiktif.
-- ==============================================================================

-- AKUN (password plain-text hanya placeholder; ganti dengan hash jika app memakai BCrypt)
INSERT INTO akun (id_akun, email, password, role, aktif) VALUES
(1,  'admin@unpar.ac.id',                    'password123', 'ADMIN',     TRUE),
(2,  'budi.santoso@unpar.ac.id',             'password123', 'DOSEN',     TRUE),
(3,  'siti.rahmawati@unpar.ac.id',           'password123', 'DOSEN',     TRUE),
(4,  'andi.wijaya@unpar.ac.id',              'password123', 'DOSEN',     TRUE),
(5,  '6182301001@student.unpar.ac.id',       'password123', 'MAHASISWA', TRUE),
(6,  '6182301002@student.unpar.ac.id',       'password123', 'MAHASISWA', TRUE),
(7,  '6182301003@student.unpar.ac.id',       'password123', 'MAHASISWA', TRUE),
(8,  '6182301004@student.unpar.ac.id',       'password123', 'MAHASISWA', TRUE),
(9,  '6182301005@student.unpar.ac.id',       'password123', 'MAHASISWA', TRUE),
(10, '6182301006@student.unpar.ac.id',       'password123', 'MAHASISWA', TRUE);

-- DOSEN
INSERT INTO dosen (nomor_induk_dosen, nama, id_akun) VALUES
('1975031501', 'Dr. Budi Santoso',     2),
('1980061202', 'Dr. Siti Rahmawati',   3),
('1983092303', 'Andi Wijaya, M.T.',    4);

-- MAHASISWA
INSERT INTO mahasiswa (npm, nama, id_akun) VALUES
('6182301001', 'Rina Maharani',   5),
('6182301002', 'Dimas Pratama',   6),
('6182301003', 'Salsa Aulia',     7),
('6182301004', 'Bagas Nugroho',   8),
('6182301005', 'Citra Lestari',   9),
('6182301006', 'Fajar Ramadhan', 10);

-- MATA KULIAH
INSERT INTO mata_kuliah (id_matkul, nama, sks) VALUES
('IF2110', 'Algoritma dan Struktur Data', 4),
('IF3130', 'Jaringan Komputer',           3),
('IF2250', 'Rekayasa Perangkat Lunak',    3);

-- MATA KULIAH DIBUKA (semester 25261)
INSERT INTO mata_kuliah_buka (id_matkul_buka, id_matkul, semester_akademik, nomor_induk_koordinator) VALUES
(1, 'IF2110', '25261', '1975031501'),
(2, 'IF3130', '25261', '1975031501'),
(3, 'IF2250', '25261', '1980061202');

-- KELAS (id 101 dan 102 cocok dengan respons API dashboard dosen)
INSERT INTO kelas (id_kelas, id_matkul_buka, kode_kelas, nomor_induk_penanggung_jawab) VALUES
(101, 1, 'A', '1975031501'),
(102, 2, 'B', '1975031501'),
(103, 3, 'A', '1980061202');

-- KULIAH (hari: 1 = Senin ... 7 = Minggu)
INSERT INTO kuliah (id_kelas, nomor_induk_pengajar, hari, jam_mulai, jam_selesai, ruangan, tipe_pertemuan) VALUES
(101, '1975031501', 1, '08:00', '10:30', 'Lab Komputer 1', 'KULIAH'),
(102, '1975031501', 3, '13:00', '15:30', 'Ruang 9122',     'KULIAH'),
(103, '1980061202', 2, '10:00', '12:30', 'Ruang 9010',     'KULIAH');

-- UJIAN (id_ujian otomatis 1..4 pada database baru)
INSERT INTO ujian (id_matkul_buka, jenis_ujian, tanggal, jam_mulai, jam_selesai, ruangan) VALUES
(1, 'UTS', '2026-10-12', '08:00', '10:00', 'Ruang 9010'),
(1, 'UAS', '2026-12-14', '08:00', '10:00', 'Ruang 9010'),
(2, 'UTS', '2026-10-13', '13:00', '15:00', 'Ruang 9122'),
(3, 'UAS', '2026-12-15', '10:00', '12:00', 'Ruang 9011');

-- PENGAWAS UJIAN
INSERT INTO pengawas_ujian (id_ujian, nomor_induk_pengawas) VALUES
(1, '1980061202'),
(2, '1983092303'),
(3, '1983092303'),
(4, '1975031501');

-- PESERTA KELAS
INSERT INTO peserta_kelas (npm, id_kelas) VALUES
('6182301001', 101),
('6182301002', 101),
('6182301003', 101),
('6182301004', 101),
('6182301005', 102),
('6182301006', 102),
('6182301001', 102),
('6182301002', 102);

-- KELOMPOK
INSERT INTO kelompok (id_kelompok, id_kelas, no_kelompok, kapasitas, durasi_presentasi, topik) VALUES
(1, 101, 1, 3, 30, 'Sistem Rekomendasi Buku'),
(2, 101, 2, 3, 30, 'Aplikasi Penjadwalan Presentasi'),
(3, 102, 1, 2, 30, 'Simulasi Routing OSPF');

-- ANGGOTA KELOMPOK (npm harus sudah terdaftar sebagai peserta di kelas yang sama)
INSERT INTO anggota_kelompok (npm, id_kelompok, id_kelas) VALUES
('6182301001', 1, 101),
('6182301002', 1, 101),
('6182301003', 2, 101),
('6182301004', 2, 101),
('6182301005', 3, 102),
('6182301006', 3, 102);

-- PRESENTASI (kelompok 3 ditandai bentrok sebagai contoh pengujian)
INSERT INTO presentasi (id_kelompok, tanggal, jam_mulai, jam_selesai, ruangan, ada_bentrok, catatan) VALUES
(1, '2026-12-01', '09:00', '09:30', 'Ruang 9010', FALSE, NULL),
(2, '2026-12-01', '09:30', '10:00', 'Ruang 9010', FALSE, NULL),
(3, '2026-12-02', '14:00', '14:30', 'Ruang 9122', TRUE,  'Contoh data bentrok untuk pengujian');

-- Sinkronkan sequence karena beberapa id diisi manual
SELECT setval(pg_get_serial_sequence('akun', 'id_akun'),                       (SELECT MAX(id_akun) FROM akun));
SELECT setval(pg_get_serial_sequence('mata_kuliah_buka', 'id_matkul_buka'),    (SELECT MAX(id_matkul_buka) FROM mata_kuliah_buka));
SELECT setval(pg_get_serial_sequence('kelas', 'id_kelas'),                     (SELECT MAX(id_kelas) FROM kelas));
SELECT setval(pg_get_serial_sequence('kelompok', 'id_kelompok'),               (SELECT MAX(id_kelompok) FROM kelompok));
