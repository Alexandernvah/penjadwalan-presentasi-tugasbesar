package com.unpar.scheduler.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dosen")
public class Dosen {

    @Id
    @Column(name = "nomor_induk_dosen", length = 10)
    private String nomorIndukDosen;

    @Column(name = "nama", nullable = false, length = 100)
    private String nama;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_akun", referencedColumnName = "id_akun", unique = true, nullable = false)
    private Akun akun;

    public Dosen() {
    }

    public Dosen(String nomorIndukDosen, String nama, Akun akun) {
        this.nomorIndukDosen = nomorIndukDosen;
        this.nama = nama;
        this.akun = akun;
    }

    public String getNomorIndukDosen() {
        return nomorIndukDosen;
    }

    public void setNomorIndukDosen(String nomorIndukDosen) {
        this.nomorIndukDosen = nomorIndukDosen;
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
