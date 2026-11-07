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
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "ujian")
public class Ujian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ujian")
    private Integer idUjian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_matkul_buka", referencedColumnName = "id_matkul_buka", nullable = false)
    private MataKuliahBuka mataKuliahBuka;

    @Column(name = "jenis_ujian", nullable = false, length = 5)
    private String jenisUjian;

    @Column(name = "tanggal", nullable = false)
    private LocalDate tanggal;

    @Column(name = "jam_mulai", nullable = false)
    private LocalTime jamMulai;

    @Column(name = "jam_selesai", nullable = false)
    private LocalTime jamSelesai;

    @Column(name = "ruangan", nullable = false, length = 20)
    private String ruangan;

    public Ujian() {
    }

    public Ujian(Integer idUjian, MataKuliahBuka mataKuliahBuka, String jenisUjian, LocalDate tanggal, LocalTime jamMulai, LocalTime jamSelesai, String ruangan) {
        this.idUjian = idUjian;
        this.mataKuliahBuka = mataKuliahBuka;
        this.jenisUjian = jenisUjian;
        this.tanggal = tanggal;
        this.jamMulai = jamMulai;
        this.jamSelesai = jamSelesai;
        this.ruangan = ruangan;
    }

    public Integer getIdUjian() {
        return idUjian;
    }

    public void setIdUjian(Integer idUjian) {
        this.idUjian = idUjian;
    }

    public MataKuliahBuka getMataKuliahBuka() {
        return mataKuliahBuka;
    }

    public void setMataKuliahBuka(MataKuliahBuka mataKuliahBuka) {
        this.mataKuliahBuka = mataKuliahBuka;
    }

    public String getJenisUjian() {
        return jenisUjian;
    }

    public void setJenisUjian(String jenisUjian) {
        this.jenisUjian = jenisUjian;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public void setTanggal(LocalDate tanggal) {
        this.tanggal = tanggal;
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
}
