package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.kelompok.BatchCreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.CreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.KelompokDetailDTO;
import com.unpar.scheduler.dto.kelompok.UpdateKelompokRequestDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KelompokRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KelompokServiceTest {

    @Mock
    private KelompokRepository kelompokRepository;
    @Mock
    private AnggotaKelompokRepository anggotaKelompokRepository;
    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private PresentasiRepository presentasiRepository;

    private KelompokService kelompokService;
    private Kelas kelasJarkom;

    @BeforeEach
    void setUp() {
        kelompokService = new KelompokService(
            kelompokRepository,
            anggotaKelompokRepository,
            kelasRepository,
            presentasiRepository
        );
        Dosen dosen = new Dosen("1975031501", "Dr. Budi Santoso", new Akun());
        MataKuliah mk = new MataKuliah("IF3130", "Jaringan Komputer", (short) 3);
        MataKuliahBuka mkb = new MataKuliahBuka(10, mk, "26271", dosen);
        kelasJarkom = new Kelas(102, mkb, "B", dosen);
    }

    @Test
    void createKelompokPersistsCustomDurationAndRejectsUnderFiveMinutes() {
        when(kelasRepository.findById(102)).thenReturn(Optional.of(kelasJarkom));
        when(kelompokRepository.save(any(Kelompok.class))).thenAnswer(inv -> {
            Kelompok k = inv.getArgument(0);
            k.setIdKelompok(10);
            return k;
        });

        KelompokDetailDTO created = kelompokService.createKelompok(
            new CreateKelompokRequestDTO(102, 1, "Topik Custom", 4, 45)
        );
        assertEquals(45, created.durasiMenit());

        assertThrows(IllegalArgumentException.class, () ->
            kelompokService.createKelompok(new CreateKelompokRequestDTO(102, 2, "Topik", 4, 4))
        );
    }

    @Test
    void batchCreateKelompokPersistsCustomDuration() {
        when(kelasRepository.findById(102)).thenReturn(Optional.of(kelasJarkom));
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(102)).thenReturn(Collections.emptyList());
        when(kelompokRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Kelompok> savedList = inv.getArgument(0);
            for (int i = 0; i < savedList.size(); i++) {
                savedList.get(i).setIdKelompok(i + 1);
                assertEquals((short) 60, savedList.get(i).getDurasiPresentasi());
            }
            when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(102)).thenReturn(savedList);
            return savedList;
        });
        when(anggotaKelompokRepository.findByKelompok_IdKelompok(any())).thenReturn(Collections.emptyList());

        List<KelompokDetailDTO> list = kelompokService.batchCreateKelompok(
            new BatchCreateKelompokRequestDTO(102, 2, 4, 60, "")
        );
        assertEquals(2, list.size());
        assertEquals(60, list.get(0).durasiMenit());
        assertEquals(60, list.get(1).durasiMenit());
        verify(kelompokRepository).saveAll(anyList());
    }

    @Test
    void updateKelompokUpdatesDurationAndGetKelompokReturnsStoredValue() {
        Kelompok existing = new Kelompok(5, kelasJarkom, (short) 1, (short) 4, "Topik Awal", (short) 20);
        when(kelompokRepository.findById(5)).thenReturn(Optional.of(existing));
        when(kelompokRepository.save(any(Kelompok.class))).thenAnswer(inv -> inv.getArgument(0));
        when(anggotaKelompokRepository.findByKelompok_IdKelompok(5)).thenReturn(Collections.emptyList());

        KelompokDetailDTO updated = kelompokService.updateKelompok(
            5,
            new UpdateKelompokRequestDTO("Topik Baru", 5, 45)
        );
        assertEquals(45, updated.durasiMenit());
        assertEquals((short) 45, existing.getDurasiPresentasi());

        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(102)).thenReturn(List.of(existing));
        List<KelompokDetailDTO> fetched = kelompokService.getKelompokByKelas(102);
        assertEquals(1, fetched.size());
        assertEquals(45, fetched.get(0).durasiMenit());
    }
}
