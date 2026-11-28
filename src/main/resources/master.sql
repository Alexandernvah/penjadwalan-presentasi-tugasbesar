-- ==============================================================================
-- Master DDL Schema: db_penjadwalan
-- Sistem Penjadwalan Presentasi Tugas Besar - Teknik Informatika UNPAR
-- Jalankan file ini terlebih dahulu sebelum dummy_data.sql
-- ==============================================================================

DROP TABLE IF EXISTS presentasi CASCADE;
DROP TABLE IF EXISTS anggota_kelompok CASCADE;
DROP TABLE IF EXISTS kelompok CASCADE;
DROP TABLE IF EXISTS peserta_kelas CASCADE;
DROP TABLE IF EXISTS pengawas_ujian CASCADE;
DROP TABLE IF EXISTS ujian CASCADE;
DROP TABLE IF EXISTS kuliah CASCADE;
DROP TABLE IF EXISTS kelas CASCADE;
DROP TABLE IF EXISTS mata_kuliah_buka CASCADE;
DROP TABLE IF EXISTS mata_kuliah CASCADE;
DROP TABLE IF EXISTS mahasiswa CASCADE;
DROP TABLE IF EXISTS dosen CASCADE;
DROP TABLE IF EXISTS akun CASCADE;

CREATE TABLE akun (
    id_akun SERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'DOSEN', 'MAHASISWA')),
    aktif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE dosen (
    nomor_induk_dosen CHAR(10) PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    id_akun INT NOT NULL UNIQUE,
    CONSTRAINT fk_dosen_akun FOREIGN KEY (id_akun) REFERENCES akun(id_akun) ON DELETE CASCADE
);

CREATE TABLE mahasiswa (
    npm CHAR(10) PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    id_akun INT NOT NULL UNIQUE,
    CONSTRAINT fk_mahasiswa_akun FOREIGN KEY (id_akun) REFERENCES akun(id_akun) ON DELETE CASCADE
);

CREATE TABLE mata_kuliah (
    id_matkul VARCHAR(10) PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    sks SMALLINT NOT NULL CHECK (sks > 0)
);

CREATE TABLE mata_kuliah_buka (
    id_matkul_buka SERIAL PRIMARY KEY,
    id_matkul VARCHAR(10) NOT NULL,
    semester_akademik CHAR(5) NOT NULL,
    nomor_induk_koordinator CHAR(10) NOT NULL,
    CONSTRAINT fk_mkbuka_matkul FOREIGN KEY (id_matkul) REFERENCES mata_kuliah(id_matkul) ON DELETE CASCADE,
    CONSTRAINT fk_mkbuka_koordinator FOREIGN KEY (nomor_induk_koordinator) REFERENCES dosen(nomor_induk_dosen),
    CONSTRAINT uq_matkul_semester UNIQUE (id_matkul, semester_akademik)
);

CREATE TABLE kelas (
    id_kelas SERIAL PRIMARY KEY,
    id_matkul_buka INT NOT NULL,
    kode_kelas VARCHAR(5) NOT NULL,
    nomor_induk_penanggung_jawab CHAR(10) NULL,
    CONSTRAINT fk_kelas_mkbuka FOREIGN KEY (id_matkul_buka) REFERENCES mata_kuliah_buka(id_matkul_buka) ON DELETE CASCADE,
    CONSTRAINT fk_kelas_dosen_pj FOREIGN KEY (nomor_induk_penanggung_jawab) REFERENCES dosen(nomor_induk_dosen),
    CONSTRAINT uq_kelas_per_buka UNIQUE (id_matkul_buka, kode_kelas)
);

CREATE TABLE kuliah (
    id_kuliah SERIAL PRIMARY KEY,
    id_kelas INT NOT NULL,
    nomor_induk_pengajar CHAR(10) NOT NULL,
    hari SMALLINT NOT NULL CHECK (hari BETWEEN 1 AND 7),
    jam_mulai TIME NOT NULL,
    jam_selesai TIME NOT NULL,
    ruangan VARCHAR(20) NOT NULL,
    tipe_pertemuan VARCHAR(20) NOT NULL,
    CONSTRAINT chk_jam_kuliah CHECK (jam_selesai > jam_mulai),
    CONSTRAINT fk_kuliah_kelas FOREIGN KEY (id_kelas) REFERENCES kelas(id_kelas) ON DELETE CASCADE,
    CONSTRAINT fk_kuliah_pengajar FOREIGN KEY (nomor_induk_pengajar) REFERENCES dosen(nomor_induk_dosen)
);

CREATE TABLE ujian (
    id_ujian SERIAL PRIMARY KEY,
    id_matkul_buka INT NOT NULL,
    jenis_ujian VARCHAR(5) NOT NULL CHECK (jenis_ujian IN ('UTS', 'UAS')),
    tanggal DATE NOT NULL,
    jam_mulai TIME NOT NULL,
    jam_selesai TIME NOT NULL,
    ruangan VARCHAR(20) NOT NULL,
    CONSTRAINT chk_jam_ujian CHECK (jam_selesai > jam_mulai),
    CONSTRAINT fk_ujian_mkbuka FOREIGN KEY (id_matkul_buka) REFERENCES mata_kuliah_buka(id_matkul_buka) ON DELETE CASCADE
);

CREATE TABLE pengawas_ujian (
    id_ujian INT NOT NULL,
    nomor_induk_pengawas CHAR(10) NOT NULL,
    PRIMARY KEY (id_ujian, nomor_induk_pengawas),
    CONSTRAINT fk_pengawas_ujian FOREIGN KEY (id_ujian) REFERENCES ujian(id_ujian) ON DELETE CASCADE,
    CONSTRAINT fk_pengawas_dosen FOREIGN KEY (nomor_induk_pengawas) REFERENCES dosen(nomor_induk_dosen)
);

CREATE TABLE peserta_kelas (
    id_peserta SERIAL PRIMARY KEY,
    npm CHAR(10) NOT NULL,
    id_kelas INT NOT NULL,
    CONSTRAINT uq_peserta_kelas UNIQUE (npm, id_kelas),
    CONSTRAINT fk_peserta_mhs FOREIGN KEY (npm) REFERENCES mahasiswa(npm) ON DELETE CASCADE,
    CONSTRAINT fk_peserta_kelas FOREIGN KEY (id_kelas) REFERENCES kelas(id_kelas) ON DELETE CASCADE
);

CREATE TABLE kelompok (
    id_kelompok SERIAL PRIMARY KEY,
    id_kelas INT NOT NULL,
    no_kelompok SMALLINT NOT NULL CHECK (no_kelompok > 0),
    kapasitas SMALLINT NOT NULL CHECK (kapasitas > 0),
    durasi_presentasi SMALLINT NOT NULL CHECK (durasi_presentasi >= 5),
    topik VARCHAR(200) NULL,
    CONSTRAINT uq_kelompok_kelas UNIQUE (id_kelas, no_kelompok),
    CONSTRAINT uq_kelompok_target_fk UNIQUE (id_kelompok, id_kelas),
    CONSTRAINT fk_kelompok_kelas FOREIGN KEY (id_kelas) REFERENCES kelas(id_kelas) ON DELETE CASCADE
);

CREATE TABLE anggota_kelompok (
    id_anggota SERIAL PRIMARY KEY,
    npm CHAR(10) NOT NULL,
    id_kelompok INT NOT NULL,
    id_kelas INT NOT NULL,
    CONSTRAINT uq_anggota_kelas UNIQUE (npm, id_kelas),
    CONSTRAINT fk_anggota_kelompok_composite FOREIGN KEY (id_kelompok, id_kelas)
        REFERENCES kelompok(id_kelompok, id_kelas) ON DELETE CASCADE,
    CONSTRAINT fk_anggota_peserta_composite FOREIGN KEY (npm, id_kelas)
        REFERENCES peserta_kelas(npm, id_kelas) ON DELETE CASCADE
);

CREATE TABLE presentasi (
    id_presentasi SERIAL PRIMARY KEY,
    id_kelompok INT NOT NULL UNIQUE,
    tanggal DATE NOT NULL,
    jam_mulai TIME NOT NULL,
    jam_selesai TIME NOT NULL,
    ruangan VARCHAR(20) NOT NULL,
    ada_bentrok BOOLEAN NOT NULL DEFAULT FALSE,
    catatan TEXT NULL,
    CONSTRAINT chk_jam_presentasi CHECK (jam_selesai > jam_mulai),
    CONSTRAINT fk_presentasi_kelompok FOREIGN KEY (id_kelompok) REFERENCES kelompok(id_kelompok) ON DELETE CASCADE
);

CREATE INDEX idx_kuliah_pengajar ON kuliah(nomor_induk_pengajar);
CREATE INDEX idx_kuliah_jadwal ON kuliah(hari, jam_mulai, jam_selesai);
CREATE INDEX idx_ujian_jadwal ON ujian(tanggal, jam_mulai, jam_selesai);
CREATE INDEX idx_pengawas_dosen ON pengawas_ujian(nomor_induk_pengawas);
CREATE INDEX idx_peserta_mhs ON peserta_kelas(npm);
CREATE INDEX idx_peserta_kelas ON peserta_kelas(id_kelas);
CREATE INDEX idx_anggota_kelompok ON anggota_kelompok(id_kelompok);
CREATE INDEX idx_presentasi_jadwal ON presentasi(tanggal, jam_mulai, jam_selesai);
CREATE INDEX idx_kelas_mkbuka ON kelas(id_matkul_buka);
