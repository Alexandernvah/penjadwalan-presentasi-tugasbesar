package com.unpar.scheduler.dto.mahasiswa;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KelompokTersediaDTO(
    Integer idKelompok,
    Short noKelompok,
    String topik,
    Short kapasitas,
    int jumlahAnggota,
    Short durasiPresentasi,
    boolean sudahTerjadwal,
    @JsonProperty("isAnggotaSaya") boolean isAnggotaSaya,
    @JsonProperty("isPenuh") boolean isPenuh,
    String statusLabel,
    List<AnggotaKelompokItemDTO> daftarAnggota
) {
}
