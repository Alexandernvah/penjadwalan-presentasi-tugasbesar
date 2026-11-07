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
    name = "kelompok",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_kelompok_kelas", columnNames = {"id_kelas", "no_kelompok"}),
        @UniqueConstraint(name = "uq_kelompok_target_fk", columnNames = {"id_kelompok", "id_kelas"})
    }
)
public class Kelompok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kelompok")
    private Integer idKelompok;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_kelas", referencedColumnName = "id_kelas", nullable = false)
    private Kelas kelas;

    @Column(name = "no_kelompok", nullable = false)
    private Short noKelompok;

    @Column(name = "kapasitas", nullable = false)
    private Short kapasitas;

    @Column(name = "topik", length = 200)
    private String topik;

    public Kelompok() {
    }

    public Kelompok(Integer idKelompok, Kelas kelas, Short noKelompok, Short kapasitas, String topik) {
        this.idKelompok = idKelompok;
        this.kelas = kelas;
        this.noKelompok = noKelompok;
        this.kapasitas = kapasitas;
        this.topik = topik;
    }

    public Integer getIdKelompok() {
        return idKelompok;
    }

    public void setIdKelompok(Integer idKelompok) {
        this.idKelompok = idKelompok;
    }

    public Kelas getKelas() {
        return kelas;
    }

    public void setKelas(Kelas kelas) {
        this.kelas = kelas;
    }

    public Short getNoKelompok() {
        return noKelompok;
    }

    public void setNoKelompok(Short noKelompok) {
        this.noKelompok = noKelompok;
    }

    public Short getKapasitas() {
        return kapasitas;
    }

    public void setKapasitas(Short kapasitas) {
        this.kapasitas = kapasitas;
    }

    public String getTopik() {
        return topik;
    }

    public void setTopik(String topik) {
        this.topik = topik;
    }
}
