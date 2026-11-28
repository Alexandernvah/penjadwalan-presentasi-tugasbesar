package com.unpar.scheduler.dto.matkul;

import java.util.List;

public record MatkulTersediaResponseDTO (String semester, String semesterLabel, boolean terkunci, List<MatkulItemDTO> matkul){
    
}
