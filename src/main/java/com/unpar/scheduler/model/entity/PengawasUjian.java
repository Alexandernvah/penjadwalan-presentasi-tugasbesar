package com.unpar.scheduler.model.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "pengawas_ujian")
public class PengawasUjian {

    @EmbeddedId
    private PengawasUjianId id;

    @MapsId("idUjian")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ujian", referencedColumnName = "id_ujian", nullable = false)
    private Ujian ujian;

    @MapsId("nomorIndukDosen")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nomor_induk_pengawas", referencedColumnName = "nomor_induk_dosen", nullable = false)
    private Dosen dosen;

    public PengawasUjian() {
    }

    public PengawasUjian(PengawasUjianId id, Ujian ujian, Dosen dosen) {
        this.id = id;
        this.ujian = ujian;
        this.dosen = dosen;
    }

    public PengawasUjian(Ujian ujian, Dosen dosen) {
        this.ujian = ujian;
        this.dosen = dosen;
        this.id = new PengawasUjianId(
            ujian != null ? ujian.getIdUjian() : null,
            dosen != null ? dosen.getNomorIndukDosen() : null
        );
    }

    public PengawasUjianId getId() {
        return id;
    }

    public void setId(PengawasUjianId id) {
        this.id = id;
    }

    public Ujian getUjian() {
        return ujian;
    }

    public void setUjian(Ujian ujian) {
        this.ujian = ujian;
        if (ujian != null) {
            if (this.id == null) {
                this.id = new PengawasUjianId();
            }
            this.id.setIdUjian(ujian.getIdUjian());
        }
    }

    public Dosen getDosen() {
        return dosen;
    }

    public void setDosen(Dosen dosen) {
        this.dosen = dosen;
        if (dosen != null) {
            if (this.id == null) {
                this.id = new PengawasUjianId();
            }
            this.id.setNomorIndukDosen(dosen.getNomorIndukDosen());
        }
    }
}
