package com.unpar.scheduler.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PengawasUjianId implements Serializable {

    @Column(name = "id_ujian")
    private Integer idUjian;

    @Column(name = "nomor_induk_pengawas", length = 10)
    private String nomorIndukDosen;

    public PengawasUjianId() {
    }

    public PengawasUjianId(Integer idUjian, String nomorIndukDosen) {
        this.idUjian = idUjian;
        this.nomorIndukDosen = nomorIndukDosen;
    }

    public Integer getIdUjian() {
        return idUjian;
    }

    public void setIdUjian(Integer idUjian) {
        this.idUjian = idUjian;
    }

    public String getNomorIndukDosen() {
        return nomorIndukDosen;
    }

    public void setNomorIndukDosen(String nomorIndukDosen) {
        this.nomorIndukDosen = nomorIndukDosen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PengawasUjianId that = (PengawasUjianId) o;
        return Objects.equals(idUjian, that.idUjian) && Objects.equals(nomorIndukDosen, that.nomorIndukDosen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUjian, nomorIndukDosen);
    }
}
