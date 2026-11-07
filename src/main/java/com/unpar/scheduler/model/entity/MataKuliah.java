package com.unpar.scheduler.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mata_kuliah")
public class MataKuliah {

    @Id
    @Column(name = "id_matkul", length = 10)
    private String idMatkul;

    @Column(name = "nama", nullable = false, length = 100)
    private String nama;

    @Column(name = "sks", nullable = false)
    private Short sks;

    public MataKuliah() {
    }

    public MataKuliah(String idMatkul, String nama, Short sks) {
        this.idMatkul = idMatkul;
        this.nama = nama;
        this.sks = sks;
    }

    public String getIdMatkul() {
        return idMatkul;
    }

    public void setIdMatkul(String idMatkul) {
        this.idMatkul = idMatkul;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public Short getSks() {
        return sks;
    }

    public void setSks(Short sks) {
        this.sks = sks;
    }
}
