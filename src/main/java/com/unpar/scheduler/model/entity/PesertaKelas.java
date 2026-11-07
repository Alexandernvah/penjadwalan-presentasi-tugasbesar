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
    name = "peserta_kelas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_peserta_kelas", columnNames = {"npm", "id_kelas"})
    }
)
public class PesertaKelas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_peserta")
    private Integer idPeserta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "npm", referencedColumnName = "npm", nullable = false)
    private Mahasiswa mahasiswa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_kelas", referencedColumnName = "id_kelas", nullable = false)
    private Kelas kelas;

    public PesertaKelas() {
    }

    public PesertaKelas(Integer idPeserta, Mahasiswa mahasiswa, Kelas kelas) {
        this.idPeserta = idPeserta;
        this.mahasiswa = mahasiswa;
        this.kelas = kelas;
    }

    public Integer getIdPeserta() {
        return idPeserta;
    }

    public void setIdPeserta(Integer idPeserta) {
        this.idPeserta = idPeserta;
    }

    public Mahasiswa getMahasiswa() {
        return mahasiswa;
    }

    public void setMahasiswa(Mahasiswa mahasiswa) {
        this.mahasiswa = mahasiswa;
    }

    public Kelas getKelas() {
        return kelas;
    }

    public void setKelas(Kelas kelas) {
        this.kelas = kelas;
    }
}
