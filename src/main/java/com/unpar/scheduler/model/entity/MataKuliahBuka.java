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
    name = "mata_kuliah_buka",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_matkul_semester", columnNames = {"id_matkul", "semester_akademik"})
    }
)
public class MataKuliahBuka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_matkul_buka")
    private Integer idMatkulBuka;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_matkul", referencedColumnName = "id_matkul", nullable = false)
    private MataKuliah matakuliah;

    @Column(name = "semester_akademik", nullable = false, length = 5)
    private String semesterAkademik;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nomor_induk_koordinator", referencedColumnName = "nomor_induk_dosen", nullable = false)
    private Dosen koordinator;

    public MataKuliahBuka() {
    }

    public MataKuliahBuka(Integer idMatkulBuka, MataKuliah matakuliah, String semesterAkademik, Dosen koordinator) {
        this.idMatkulBuka = idMatkulBuka;
        this.matakuliah = matakuliah;
        this.semesterAkademik = semesterAkademik;
        this.koordinator = koordinator;
    }

    public Integer getIdMatkulBuka() {
        return idMatkulBuka;
    }

    public void setIdMatkulBuka(Integer idMatkulBuka) {
        this.idMatkulBuka = idMatkulBuka;
    }

    public MataKuliah getMatakuliah() {
        return matakuliah;
    }

    public void setMatakuliah(MataKuliah matakuliah) {
        this.matakuliah = matakuliah;
    }

    public String getSemesterAkademik() {
        return semesterAkademik;
    }

    public void setSemesterAkademik(String semesterAkademik) {
        this.semesterAkademik = semesterAkademik;
    }

    public Dosen getKoordinator() {
        return koordinator;
    }

    public void setKoordinator(Dosen koordinator) {
        this.koordinator = koordinator;
    }
}
