package com.unpar.scheduler.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "presentasi")
public class Presentasi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_presentasi")
    private Integer idPresentasi;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_kelompok", referencedColumnName = "id_kelompok", unique = true, nullable = false)
    private Kelompok kelompok;

    @Column(name = "tanggal", nullable = false)
    private LocalDate tanggal;

    @Column(name = "jam_mulai", nullable = false)
    private LocalTime jamMulai;

    @Column(name = "jam_selesai", nullable = false)
    private LocalTime jamSelesai;

    @Column(name = "ruangan", nullable = false, length = 20)
    private String ruangan;

    @Column(name = "ada_bentrok", nullable = false)
    private Boolean adaBentrok = false;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    public Presentasi() {
    }

    public Presentasi(Integer idPresentasi, Kelompok kelompok, LocalDate tanggal, LocalTime jamMulai, LocalTime jamSelesai, String ruangan, Boolean adaBentrok, String catatan) {
        this.idPresentasi = idPresentasi;
        this.kelompok = kelompok;
        this.tanggal = tanggal;
        this.jamMulai = jamMulai;
        this.jamSelesai = jamSelesai;
        this.ruangan = ruangan;
        this.adaBentrok = adaBentrok;
        this.catatan = catatan;
    }

    public Integer getIdPresentasi() {
        return idPresentasi;
    }

    public void setIdPresentasi(Integer idPresentasi) {
        this.idPresentasi = idPresentasi;
    }

    public Kelompok getKelompok() {
        return kelompok;
    }

    public void setKelompok(Kelompok kelompok) {
        this.kelompok = kelompok;
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

    public Boolean getAdaBentrok() {
        return adaBentrok;
    }

    public void setAdaBentrok(Boolean adaBentrok) {
        this.adaBentrok = adaBentrok;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }
}
