package com.unpar.scheduler.dto.mahasiswa;

import java.util.List;

public record JadwalPresentasiMahasiswaDTO(
    Integer idPresentasi,
    Integer idKelompok,
    Short noKelompok,
    String topik,
    String kodeMatkul,
    String namaMatkul,
    String kodeKelas,
    String namaDosen,
    String tanggal,
    String hariTanggalFormatted,
    String tanggalAngka,
    String bulanTahunLabel,
    String waktuMulai,
    String waktuSelesai,
    String rentangWaktuLabel,
    String ruangan,
    String catatan,
    String statusKode,
    String statusLabel,
    List<AnggotaKelompokItemDTO> daftarAnggota
) {
}
