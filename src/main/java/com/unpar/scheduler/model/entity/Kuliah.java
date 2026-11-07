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
import java.time.LocalTime;

@Entity
@Table(name = "kuliah")
public class Kuliah {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kuliah")
    private Integer idKuliah;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_kelas", referencedColumnName = "id_kelas", nullable = false)
    private Kelas kelas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nomor_induk_pengajar", referencedColumnName = "nomor_induk_dosen", nullable = false)
    private Dosen dosenPengajar;

    @Column(name = "hari", nullable = false)
    private Short hari;

    @Column(name = "jam_mulai", nullable = false)
    private LocalTime jamMulai;

    @Column(name = "jam_selesai", nullable = false)
    private LocalTime jamSelesai;

    @Column(name = "ruangan", nullable = false, length = 20)
    private String ruangan;

    @Column(name = "tipe_pertemuan", nullable = false, length = 20)
    private String tipePertemuan;

    public Kuliah() {
    }

    public Kuliah(Integer idKuliah, Kelas kelas, Dosen dosenPengajar, Short hari, LocalTime jamMulai, LocalTime jamSelesai, String ruangan, String tipePertemuan) {
        this.idKuliah = idKuliah;
        this.kelas = kelas;
        this.dosenPengajar = dosenPengajar;
        this.hari = hari;
        this.jamMulai = jamMulai;
        this.jamSelesai = jamSelesai;
        this.ruangan = ruangan;
        this.tipePertemuan = tipePertemuan;
    }

    public Integer getIdKuliah() {
        return idKuliah;
    }

    public void setIdKuliah(Integer idKuliah) {
        this.idKuliah = idKuliah;
    }

    public Kelas getKelas() {
        return kelas;
    }

    public void setKelas(Kelas kelas) {
        this.kelas = kelas;
    }

    public Dosen getDosenPengajar() {
        return dosenPengajar;
    }

    public void setDosenPengajar(Dosen dosenPengajar) {
        this.dosenPengajar = dosenPengajar;
    }

    public Short getHari() {
        return hari;
    }

    public void setHari(Short hari) {
        this.hari = hari;
    }

    public LocalTime getJamMulai() {
        return jamMulai;
    }

    public void setJamMulai(LocalTime jamMulai) {
        this.jamMulai = jamMulai;
    }

    public LocalTime getJamSelesai() {
        return jamSelesai;
    }

    public void setJamSelesai(LocalTime jamSelesai) {
        this.jamSelesai = jamSelesai;
    }

    public String getRuangan() {
        return ruangan;
    }

    public void setRuangan(String ruangan) {
        this.ruangan = ruangan;
    }

    public String getTipePertemuan() {
        return tipePertemuan;
    }

    public void setTipePertemuan(String tipePertemuan) {
        this.tipePertemuan = tipePertemuan;
    }
}
