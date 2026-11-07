package com.unpar.scheduler.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "mahasiswa")
public class Mahasiswa {

    @Id
    @Column(name = "npm", length = 10)
    private String npm;

    @Column(name = "nama", nullable = false, length = 100)
    private String nama;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_akun", referencedColumnName = "id_akun", unique = true, nullable = false)
    private Akun akun;

    public Mahasiswa() {
    }

    public Mahasiswa(String npm, String nama, Akun akun) {
        this.npm = npm;
        this.nama = nama;
        this.akun = akun;
    }

    public String getNpm() {
        return npm;
    }

    public void setNpm(String npm) {
        this.npm = npm;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public Akun getAkun() {
        return akun;
    }

    public void setAkun(Akun akun) {
        this.akun = akun;
    }
}
