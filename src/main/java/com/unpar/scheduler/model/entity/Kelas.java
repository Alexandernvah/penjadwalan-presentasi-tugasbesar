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
    name = "kelas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_kelas_per_buka", columnNames = {"id_matkul_buka", "kode_kelas"})
    }
)
public class Kelas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kelas")
    private Integer idKelas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_matkul_buka", referencedColumnName = "id_matkul_buka", nullable = false)
    private MataKuliahBuka mataKuliahBuka;

    @Column(name = "kode_kelas", nullable = false, length = 5)
    private String kodeKelas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nomor_induk_penanggung_jawab", referencedColumnName = "nomor_induk_dosen")
    private Dosen penanggungJawab;

    public Kelas() {
    }

    public Kelas(Integer idKelas, MataKuliahBuka mataKuliahBuka, String kodeKelas, Dosen penanggungJawab) {
        this.idKelas = idKelas;
        this.mataKuliahBuka = mataKuliahBuka;
        this.kodeKelas = kodeKelas;
        this.penanggungJawab = penanggungJawab;
    }

    public Integer getIdKelas() {
        return idKelas;
    }

    public void setIdKelas(Integer idKelas) {
        this.idKelas = idKelas;
    }

    public MataKuliahBuka getMataKuliahBuka() {
        return mataKuliahBuka;
    }

    public void setMataKuliahBuka(MataKuliahBuka mataKuliahBuka) {
        this.mataKuliahBuka = mataKuliahBuka;
    }

    public String getKodeKelas() {
        return kodeKelas;
    }

    public void setKodeKelas(String kodeKelas) {
        this.kodeKelas = kodeKelas;
    }

    public Dosen getPenanggungJawab() {
        return penanggungJawab;
    }

    public void setPenanggungJawab(Dosen penanggungJawab) {
        this.penanggungJawab = penanggungJawab;
    }
}
