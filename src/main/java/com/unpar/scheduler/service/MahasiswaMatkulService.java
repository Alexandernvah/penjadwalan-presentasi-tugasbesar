package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.mahasiswa.KelasTersediaDTO;
import com.unpar.scheduler.dto.mahasiswa.MataKuliahTersediaDTO;
import com.unpar.scheduler.dto.mahasiswa.MatkulMahasiswaResponseDTO;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.MahasiswaRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

// Menyusun daftar mata kuliah semester aktif untuk mahasiswa dan menyimpan pilihan kelasnya satu kali (terkunci).
@Service
public class MahasiswaMatkulService {

    private final KelasRepository kelasRepository;
    private final PesertaKelasRepository pesertaKelasRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final NamaDosenPengajarResolver namaDosenPengajarResolver;

    public MahasiswaMatkulService(
        KelasRepository kelasRepository,
        PesertaKelasRepository pesertaKelasRepository,
        MahasiswaRepository mahasiswaRepository,
        NamaDosenPengajarResolver namaDosenPengajarResolver
    ) {
        this.kelasRepository = kelasRepository;
        this.pesertaKelasRepository = pesertaKelasRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.namaDosenPengajarResolver = namaDosenPengajarResolver;
    }

    @Transactional(readOnly = true)
    public MatkulMahasiswaResponseDTO getMatkulTersedia(Mahasiswa mahasiswa) {
        Optional<String> semester = cariSemesterAktif();
        if (semester.isEmpty()) {
            return new MatkulMahasiswaResponseDTO(mahasiswa.getNama(), mahasiswa.getNpm().trim(), false, List.of());
        }
        return susunResponse(mahasiswa, semester.get());
    }

    @Transactional
    public MatkulMahasiswaResponseDTO simpanMatkul(Mahasiswa mahasiswa, List<Integer> idKelasDipilih) {
        String semester = cariSemesterAktif()
            .orElseThrow(() -> new MahasiswaException(HttpStatus.NOT_FOUND, "Belum ada semester aktif."));

        // Kunci baris mahasiswa dulu: submit ganda menunggu di sini, lalu ditolak oleh pengecekan kunci di bawah.
        mahasiswaRepository.findByNpmForUpdate(mahasiswa.getNpm())
            .orElseThrow(() -> new MahasiswaException(HttpStatus.NOT_FOUND, "Data mahasiswa tidak ditemukan."));

        if (!pesertaKelasRepository.findByNpmAndSemesterWithDetails(mahasiswa.getNpm(), semester).isEmpty()) {
            throw new MahasiswaException(HttpStatus.CONFLICT,
                "Mata kuliah telah dikunci. Hubungi Admin Program Studi jika terdapat kesalahan pengisian data.");
        }

        List<Kelas> kelasDipilih = validasiPilihan(idKelasDipilih, semester);
        pesertaKelasRepository.saveAll(
            kelasDipilih.stream().map(kelas -> new PesertaKelas(null, mahasiswa, kelas)).toList()
        );
        return susunResponse(mahasiswa, semester);
    }

    private List<Kelas> validasiPilihan(List<Integer> idKelasDipilih, String semester) {
        if (idKelasDipilih == null || idKelasDipilih.isEmpty() || idKelasDipilih.stream().anyMatch(Objects::isNull)) {
            throw badRequest("Pilih minimal satu mata kuliah beserta kelasnya.");
        }
        Set<Integer> idUnik = new LinkedHashSet<>(idKelasDipilih);
        if (idUnik.size() != idKelasDipilih.size()) {
            throw badRequest("Kelas yang sama dipilih lebih dari satu kali.");
        }

        List<Kelas> kelasDipilih = kelasRepository.findByIdsWithDetails(idUnik);
        if (kelasDipilih.size() != idUnik.size()) {
            throw badRequest("Ada kelas yang dipilih tidak ditemukan.");
        }

        Set<Integer> matkulBukaTerpakai = new HashSet<>();
        for (Kelas kelas : kelasDipilih) {
            if (!semester.equals(kelas.getMataKuliahBuka().getSemesterAkademik().trim())) {
                throw badRequest("Kelas yang dipilih bukan bagian dari semester aktif.");
            }
            if (!matkulBukaTerpakai.add(kelas.getMataKuliahBuka().getIdMatkulBuka())) {
                throw badRequest("Satu mata kuliah hanya boleh dipilih satu kelas.");
            }
        }
        return kelasDipilih;
    }

    private MatkulMahasiswaResponseDTO susunResponse(Mahasiswa mahasiswa, String semester) {
        List<Kelas> semuaKelas = kelasRepository.findBySemester(semester);
        Set<Integer> kelasTerpilih = pesertaKelasRepository
            .findByNpmAndSemesterWithDetails(mahasiswa.getNpm(), semester).stream()
            .map(peserta -> peserta.getKelas().getIdKelas())
            .collect(Collectors.toSet());
        Map<Integer, String> dosenPerKelas = namaDosenPengajarResolver.resolve(semuaKelas);

        // TreeMap: mata kuliah tampil terurut berdasarkan kode
        Map<String, List<Kelas>> kelasPerMatkul = new TreeMap<>();
        for (Kelas kelas : semuaKelas) {
            kelasPerMatkul
                .computeIfAbsent(kelas.getMataKuliahBuka().getMatakuliah().getIdMatkul(), kode -> new ArrayList<>())
                .add(kelas);
        }

        List<MataKuliahTersediaDTO> daftarMataKuliah = kelasPerMatkul.values().stream().map(daftarKelas -> {
            Kelas contoh = daftarKelas.get(0);
            MataKuliah matkul = contoh.getMataKuliahBuka().getMatakuliah();
            List<KelasTersediaDTO> opsiKelas = daftarKelas.stream()
                .map(kelas -> new KelasTersediaDTO(
                    kelas.getIdKelas(),
                    kelas.getKodeKelas(),
                    dosenPerKelas.getOrDefault(kelas.getIdKelas(), NamaDosenPengajarResolver.TANPA_DOSEN),
                    kelasTerpilih.contains(kelas.getIdKelas())
                ))
                .toList();
            return new MataKuliahTersediaDTO(
                contoh.getMataKuliahBuka().getIdMatkulBuka(), matkul.getIdMatkul(), matkul.getNama(), opsiKelas
            );
        }).toList();

        return new MatkulMahasiswaResponseDTO(
            mahasiswa.getNama(), mahasiswa.getNpm().trim(), !kelasTerpilih.isEmpty(), daftarMataKuliah
        );
    }

    private Optional<String> cariSemesterAktif() {
        return kelasRepository.findLatestSemesterOverall().map(String::trim).filter(kode -> !kode.isEmpty());
    }

    private static MahasiswaException badRequest(String pesan) {
        return new MahasiswaException(HttpStatus.BAD_REQUEST, pesan);
    }
}
