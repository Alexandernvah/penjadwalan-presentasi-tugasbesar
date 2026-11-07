package com.unpar.scheduler.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "anggota_kelompok",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_anggota_kelas", columnNames = {"npm", "id_kelas"})
    }
)
public class AnggotaKelompok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anggota")
    private Integer idAnggota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "npm", referencedColumnName = "npm", nullable = false)
    private Mahasiswa mahasiswa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_kelompok", referencedColumnName = "id_kelompok", nullable = false)
    private Kelompok kelompok;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_kelas", referencedColumnName = "id_kelas", nullable = false)
    private Kelas kelas;

    public AnggotaKelompok() {
    }

    public AnggotaKelompok(Integer idAnggota, Mahasiswa mahasiswa, Kelompok kelompok, Kelas kelas) {
        this.idAnggota = idAnggota;
        this.mahasiswa = mahasiswa;
        this.kelompok = kelompok;
        this.kelas = kelas;
    }

    public Integer getIdAnggota() {
        return idAnggota;
    }

    public void setIdAnggota(Integer idAnggota) {
        this.idAnggota = idAnggota;
    }

    public Mahasiswa getMahasiswa() {
        return mahasiswa;
    }

    public void setMahasiswa(Mahasiswa mahasiswa) {
        this.mahasiswa = mahasiswa;
    }

    public Kelompok getKelompok() {
        return kelompok;
    }

    public void setKelompok(Kelompok kelompok) {
        this.kelompok = kelompok;
    }

    public Kelas getKelas() {
        return kelas;
    }

    public void setKelas(Kelas kelas) {
        this.kelas = kelas;
    }
}
