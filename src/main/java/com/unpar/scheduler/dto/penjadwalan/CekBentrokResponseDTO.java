package com.unpar.scheduler.dto.penjadwalan;

import java.util.List;

public record CekBentrokResponseDTO(
    boolean adaBentrok,
    String jamMulai,
    String jamSelesai,
    Integer durasiMenit,
    List<KonflikDetailDTO> konflikList
) {
}
