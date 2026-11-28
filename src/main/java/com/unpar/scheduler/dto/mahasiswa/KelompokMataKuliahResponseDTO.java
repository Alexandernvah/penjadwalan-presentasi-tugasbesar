package com.unpar.scheduler.dto.mahasiswa;

import java.util.List;

public record KelompokMataKuliahResponseDTO(
    String namaMahasiswa,
    String npm,
    List<KelasDiambilItemDTO> daftarKelasDiambil
) {
}
