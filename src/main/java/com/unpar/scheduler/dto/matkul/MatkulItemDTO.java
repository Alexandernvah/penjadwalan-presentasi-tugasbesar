package com.unpar.scheduler.dto.matkul;

import java.util.List;

public record MatkulItemDTO(String idMatkul, String namaMatkul, Short sks, List<KelasPilihanDTO> kelas) {
    
}
