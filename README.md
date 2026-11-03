# Sistem Penjadwalan Tugas Besar Presentasi
### Teknik Informatika — Universitas Katolik Parahyangan (UNPAR)

![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![JavaScript](https://img.shields.io/badge/Vanilla%20JS-ES6+-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![CSS3](https://img.shields.io/badge/CSS3-Modular%20System-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![Apache POI](https://img.shields.io/badge/Apache%20POI-5.2.5-D22128?style=for-the-badge&logo=apache&logoColor=white)

---

## 📌 Deskripsi Proyek

**Sistem Penjadwalan Tugas Besar Presentasi** adalah platform web enterprise yang dirancang khusus untuk memfasilitasi, mengotomatisasi, dan mengoptimalkan proses penjadwalan evaluasi presentasi tugas besar pada program studi **Teknik Informatika UNPAR**. 

Sistem ini menyelesaikan tantangan kompleks seperti penentuan jadwal bebas bersama lintas anggota kelompok, matriks bentrok waktu kuliah/dosen, serta alokasi ruangan dan dosen penguji/pembimbing secara transparan dan akurat.

---

## 🚀 Fitur Utama Berdasarkan Peran

### 1. 🛡️ Portal Administrator
- **Excel Batch Import & Sync**: Unggah daftar mata kuliah, kelas, data dosen, mahasiswa, dan pemetaan kelompok via file template spreadsheet (`.xlsx`).
- **User & Account Management**: Manajemen kredensial, aktivasi akun, dan pemulihan akses pengguna.
- **Master Data Control**: Pengelolaan ruangan, kapasitas, slot waktu institusi, dan parameter semester aktif.
- **Audit Logging & Activity Tracking**: Monitoring riwayat perubahan jadwal dan aktivitas sistem secara terpusat.

### 2. 👨‍🏫 Portal Dosen Pengampu & Penguji
- **Conflict Matrix & Availability Grid**: Visualisasi ketersediaan waktu mengajar, jadwal bimbingan, dan jam berhalangan secara interaktif.
- **Automated Schedule Generator**: Algoritma alokasi jadwal presentasi kelompok bebas bentrok.
- **Manual Slot Override**: Fleksibilitas untuk memindahkan atau menukar slot waktu (*reschedule*) jika terdapat kendala operasional mendadak.
- **Kelola Kelompok & Topik**: Monitoring anggota kelompok, topik tugas besar, dan status kesiapan presentasi.
- **Ekspor Jadwal**: Unduh rekapitulasi jadwal presentasi per kelas dalam format spreadsheet resmi.

### 3. 🎓 Portal Mahasiswa
- **Input Jadwal Kuliah & Ketersediaan**: Pengisian jadwal rutin perkuliahan mingguan untuk membentuk matriks jadwal bebas kelompok (*free-time union*).
- **Team Status Dashboard**: Pemantauan status anggota tim, persetujuan topik, dan notifikasi jadwal presentasi terbit.
- **Ruang & Slot Viewer**: Tampilan transparan rincian waktu, ruangan, dan dosen penguji yang ditugaskan.


---

## 📂 Struktur Direktori Proyek

```text
rpl/
├── pom.xml                                      # Maven Configuration (Java 21, Spring Boot 3)
├── README.md                                    # Dokumentasi Proyek & Setup Guide
├── product_requirements_document_...md          # Spesifikasi Kebutuhan Perangkat Lunak (PRD)
└── src/
    └── main/
        ├── java/                                # Source Code Backend (Spring Boot)
        │   └── id/ac/unpar/informatika/tubes/
        │       ├── config/                      # Security & Web MVC Config
        │       ├── controller/                  # REST API Endpoints
        │       ├── model/                       # JPA Entities
        │       ├── repository/                  # Spring Data Repositories
        │       └── service/                     # Business Logic & Algorithms
        └── resources/
            ├── application.yml                  # Konfigurasi Database & Profil Aplikasi
            └── static/                          # Frontend Static Assets
                ├── admin/
                │   ├── dashboard.html           # Admin Dashboard & Data Controls
                │   └── index.html               # Admin Login Portal
                ├── dosen/
                │   ├── index.html               # Dosen Login Portal
                │   ├── kelola-kelompok.html     # Manajemen Kelompok & Topik
                │   ├── penjadwalan.html         # Conflict Matrix & Slot Scheduling
                │   └── jadwal-pribadi.html      # Kalender Jadwal Dosen
                ├── mahasiswa/
                │   ├── index.html               # Mahasiswa Login Portal
                │   ├── dashboard.html           # Status Kelompok & Jadwal Presentasi
                │   └── input-jadwal.html        # Input Matriks Jam Kuliah
                ├── css/
                │   ├── main.css                 # Master Design System, Auth & Utilities
                │   ├── admin.css                # Style Modul Administrator
                │   ├── dosen.css                # Style Modul Dosen & Penjadwalan
                │   └── mahasiswa.css            # Style Modul Mahasiswa
                ├── js/
                │   └── api.js                   # Client-side Fetch & JWT API Wrapper
                └── images/
                    ├── logo-unpar.png           # Lambang Resmi UNPAR
                    └── LogoInformatika2.jpg     # Logo Teknik Informatika UNPAR
```

---

## 🛠️ Prasyarat Sistem & Setup Lokal

### 1. Prasyarat
- **Java Development Kit (JDK)**: Versi 21 LTS (`java -version`).
- **Apache Maven**: Versi 3.9+ (`mvn -version`).
- **PostgreSQL Database Server**: Versi 15+ (`psql --version`).

### 2. Konfigurasi Basis Data PostgreSQL
Buat database baru melalui terminal PostgreSQL atau pgAdmin:
```sql
CREATE DATABASE tubes_scheduler;
```

Konfigurasi koneksi database telah disiapkan pada `src/main/resources/application.yml`. Anda dapat menggunakan environment variable atau menyesuaikan file konfigurasi:
- `DB_HOST`: Host basis data (default: `localhost`)
- `DB_PORT`: Port PostgreSQL (default: `5432`)
- `DB_NAME`: Nama basis data (default: `tubes_scheduler`)
- `DB_USER`: Username basis data (default: `postgres`)
- `DB_PASSWORD`: Password basis data (default: `postgres`)

### 3. Menjalankan Aplikasi via Maven
```bash
# Bersihkan dan compile proyek
mvn clean install -DskipTests

# Jalankan Spring Boot Application
mvn spring-boot:run
```

Aplikasi dapat diakses melalui browser pada port default `8080`:
```text
http://localhost:8080/
```

### 4. Rute Halaman Antarmuka (Static Frontend)
- **Portal Mahasiswa**: `http://localhost:8080/mahasiswa/index.html`
- **Portal Dosen**: `http://localhost:8080/dosen/index.html`
- **Portal Administrator**: `http://localhost:8080/admin/index.html`
