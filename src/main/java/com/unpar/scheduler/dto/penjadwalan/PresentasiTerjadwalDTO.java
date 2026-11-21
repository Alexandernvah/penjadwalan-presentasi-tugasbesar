package com.unpar.scheduler.dto.penjadwalan;

import com.unpar.scheduler.dto.kelompok.AnggotaItemDTO;
import java.util.List;

public record PresentasiTerjadwalDTO(
    Integer idPresentasi,
    Integer idKelompok,
    Integer nomorKelompok,
    String topik,
    String tanggal,
    String jamMulai,
    String jamSelesai,
    String ruangan,
    Integer durasiMenit,
    String catatan,
    String kodeKelas,
    String namaMatkul,
    List<AnggotaItemDTO> anggota
) {
}
