package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.mahasiswa.AnggotaKelompokItemDTO;
import com.unpar.scheduler.dto.mahasiswa.DaftarKelompokKelasResponseDTO;
import com.unpar.scheduler.dto.mahasiswa.KelasDiambilItemDTO;
import com.unpar.scheduler.dto.mahasiswa.KelompokMataKuliahResponseDTO;
import com.unpar.scheduler.dto.mahasiswa.KelompokTersediaDTO;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KelompokRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Mengelola daftar kelas yang diambil mahasiswa serta pendaftaran (gabung dan keluar) kelompok tugas besar.
@Service
public class MahasiswaKelompokService {

    static final String STATUS_KELOMPOK_ANDA = "Kelompok Anda";
    static final String STATUS_PENUH = "Penuh";
    static final String STATUS_TERSEDIA = "Tersedia";

    private final SemesterAkademikResolver semesterAkademikResolver;
    private final PesertaKelasRepository pesertaKelasRepository;
    private final KelasRepository kelasRepository;
    private final KelompokRepository kelompokRepository;
    private final AnggotaKelompokRepository anggotaKelompokRepository;
    private final PresentasiRepository presentasiRepository;

    public MahasiswaKelompokService(
        SemesterAkademikResolver semesterAkademikResolver,
        PesertaKelasRepository pesertaKelasRepository,
        KelasRepository kelasRepository,
        KelompokRepository kelompokRepository,
        AnggotaKelompokRepository anggotaKelompokRepository,
        PresentasiRepository presentasiRepository
    ) {
        this.semesterAkademikResolver = semesterAkademikResolver;
        this.pesertaKelasRepository = pesertaKelasRepository;
        this.kelasRepository = kelasRepository;
        this.kelompokRepository = kelompokRepository;
        this.anggotaKelompokRepository = anggotaKelompokRepository;
        this.presentasiRepository = presentasiRepository;
    }

    @Transactional(readOnly = true)
    public KelompokMataKuliahResponseDTO getMataKuliahDiambil(Mahasiswa mahasiswa) {
        String npmBersih = mahasiswa.getNpm().trim();
        Optional<String> semester = semesterAkademikResolver.resolveActiveSemesterForMahasiswa(npmBersih);
        if (semester.isEmpty()) {
            return new KelompokMataKuliahResponseDTO(mahasiswa.getNama(), npmBersih, List.of());
        }

        List<KelasDiambilItemDTO> daftarKelas = pesertaKelasRepository
            .findByNpmAndSemesterWithDetails(mahasiswa.getNpm(), semester.get()).stream()
            .map(PesertaKelas::getKelas)
            .sorted(Comparator
                .comparing((Kelas k) -> k.getMataKuliahBuka().getMatakuliah().getIdMatkul())
                .thenComparing(Kelas::getKodeKelas))
            .map(kelas -> {
                MataKuliah mk = kelas.getMataKuliahBuka().getMatakuliah();
                String label = mk.getIdMatkul() + " - " + mk.getNama() + " (Kelas " + kelas.getKodeKelas() + ")";
                return new KelasDiambilItemDTO(kelas.getIdKelas(), mk.getIdMatkul(), mk.getNama(), kelas.getKodeKelas(), label);
            })
            .toList();

        return new KelompokMataKuliahResponseDTO(mahasiswa.getNama(), npmBersih, daftarKelas);
    }

    @Transactional(readOnly = true)
    public DaftarKelompokKelasResponseDTO getDaftarKelompok(Mahasiswa mahasiswa, Integer idKelas) {
        if (idKelas == null) {
            throw new MahasiswaException(HttpStatus.BAD_REQUEST, "Parameter idKelas wajib diisi.");
        }

        Kelas kelas = kelasRepository.findByIdWithDetails(idKelas)
            .orElseThrow(() -> new MahasiswaException(HttpStatus.NOT_FOUND, "Kelas tidak ditemukan."));

        if (!pesertaKelasRepository.existsByMahasiswa_NpmAndKelas_IdKelas(mahasiswa.getNpm(), idKelas)) {
            throw new MahasiswaException(HttpStatus.FORBIDDEN, "Anda tidak terdaftar sebagai peserta pada kelas ini.");
        }

        MataKuliah mk = kelas.getMataKuliahBuka().getMatakuliah();
        List<Kelompok> kelompokList = kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(idKelas);
        if (kelompokList.isEmpty()) {
            return new DaftarKelompokKelasResponseDTO(
                kelas.getIdKelas(), mk.getIdMatkul(), mk.getNama(), kelas.getKodeKelas(), null, null, List.of()
            );
        }

        List<Integer> kelompokIds = kelompokList.stream().map(Kelompok::getIdKelompok).toList();
        Map<Integer, List<AnggotaKelompok>> anggotaByKelompok = anggotaKelompokRepository
            .findByKelompokIdsWithMahasiswa(kelompokIds).stream()
            .collect(Collectors.groupingBy(ak -> ak.getKelompok().getIdKelompok()));
        Set<Integer> terjadwalIds = presentasiRepository.findByKelasIdWithDetails(idKelas).stream()
            .map(p -> p.getKelompok().getIdKelompok())
            .collect(Collectors.toSet());

        String npmAktif = mahasiswa.getNpm().trim();
        Integer idKelompokTerdaftar = null;
        Short noKelompokTerdaftar = null;
        List<KelompokTersediaDTO> hasil = new ArrayList<>(kelompokList.size());

        for (Kelompok kelompok : kelompokList) {
            List<AnggotaKelompokItemDTO> daftarAnggota = anggotaByKelompok
                .getOrDefault(kelompok.getIdKelompok(), List.of()).stream()
                .map(ak -> toAnggotaItem(ak, npmAktif))
                .sorted(Comparator
                    .comparing(AnggotaKelompokItemDTO::isCurrentUser).reversed()
                    .thenComparing(AnggotaKelompokItemDTO::nama, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AnggotaKelompokItemDTO::npm))
                .toList();

            int jumlahAnggota = daftarAnggota.size();
            int batasKapasitas = kelompok.getKapasitas() != null ? kelompok.getKapasitas() : 0;
            boolean isAnggotaSaya = daftarAnggota.stream().anyMatch(AnggotaKelompokItemDTO::isCurrentUser);
            boolean isPenuh = jumlahAnggota >= batasKapasitas;
            boolean sudahTerjadwal = terjadwalIds.contains(kelompok.getIdKelompok());

            if (isAnggotaSaya) {
                idKelompokTerdaftar = kelompok.getIdKelompok();
                noKelompokTerdaftar = kelompok.getNoKelompok();
            }

            String statusLabel = isAnggotaSaya ? STATUS_KELOMPOK_ANDA : (isPenuh ? STATUS_PENUH : STATUS_TERSEDIA);
            hasil.add(new KelompokTersediaDTO(
                kelompok.getIdKelompok(),
                kelompok.getNoKelompok(),
                kelompok.getTopik(),
                kelompok.getKapasitas(),
                jumlahAnggota,
                kelompok.getDurasiPresentasi(),
                sudahTerjadwal,
                isAnggotaSaya,
                isPenuh,
                statusLabel,
                daftarAnggota
            ));
        }

        return new DaftarKelompokKelasResponseDTO(
            kelas.getIdKelas(), mk.getIdMatkul(), mk.getNama(), kelas.getKodeKelas(),
            idKelompokTerdaftar, noKelompokTerdaftar, hasil
        );
    }

    @Transactional
    public DaftarKelompokKelasResponseDTO gabungKelompok(Mahasiswa mahasiswa, Integer idKelompok) {
        if (idKelompok == null) {
            throw new MahasiswaException(HttpStatus.BAD_REQUEST, "Kelompok tidak valid.");
        }

        Kelompok kelompok = kelompokRepository.findByIdForUpdate(idKelompok)
            .orElseThrow(() -> new MahasiswaException(HttpStatus.NOT_FOUND, "Kelompok tidak ditemukan."));

        Integer idKelas = kelompok.getKelas().getIdKelas();
        if (!pesertaKelasRepository.existsByMahasiswa_NpmAndKelas_IdKelas(mahasiswa.getNpm(), idKelas)) {
            throw new MahasiswaException(HttpStatus.FORBIDDEN, "Anda tidak terdaftar sebagai peserta pada kelas ini.");
        }

        if (anggotaKelompokRepository.existsByMahasiswa_NpmAndKelas_IdKelas(mahasiswa.getNpm(), idKelas)) {
            throw new MahasiswaException(HttpStatus.CONFLICT, "Anda sudah tergabung dalam kelompok pada kelas ini.");
        }

        long jumlahSaatIni = anggotaKelompokRepository.countByKelompok_IdKelompok(idKelompok);
        int kapasitas = kelompok.getKapasitas() != null ? kelompok.getKapasitas() : 0;
        if (jumlahSaatIni >= kapasitas) {
            throw new MahasiswaException(HttpStatus.CONFLICT, "Kuota kelompok sudah penuh.");
        }

        anggotaKelompokRepository.saveAndFlush(new AnggotaKelompok(null, mahasiswa, kelompok, kelompok.getKelas()));
        return getDaftarKelompok(mahasiswa, idKelas);
    }

    @Transactional
    public DaftarKelompokKelasResponseDTO keluarKelompok(Mahasiswa mahasiswa, Integer idKelompok) {
        if (idKelompok == null) {
            throw new MahasiswaException(HttpStatus.BAD_REQUEST, "Kelompok tidak valid.");
        }

        Kelompok kelompok = kelompokRepository.findByIdForUpdate(idKelompok)
            .orElseThrow(() -> new MahasiswaException(HttpStatus.NOT_FOUND, "Kelompok tidak ditemukan."));

        AnggotaKelompok keanggotaan = anggotaKelompokRepository
            .findByMahasiswa_NpmAndKelompok_IdKelompok(mahasiswa.getNpm(), idKelompok)
            .orElseThrow(() -> new MahasiswaException(HttpStatus.NOT_FOUND, "Anda bukan anggota kelompok ini."));

        if (presentasiRepository.existsByKelompok_IdKelompok(idKelompok)) {
            throw new MahasiswaException(HttpStatus.CONFLICT,
                "Tidak dapat keluar karena jadwal presentasi kelompok sudah ditetapkan oleh dosen.");
        }

        anggotaKelompokRepository.delete(keanggotaan);
        anggotaKelompokRepository.flush();
        return getDaftarKelompok(mahasiswa, kelompok.getKelas().getIdKelas());
    }

    private static AnggotaKelompokItemDTO toAnggotaItem(AnggotaKelompok ak, String npmAktif) {
        String npmAnggota = ak.getMahasiswa().getNpm().trim();
        return new AnggotaKelompokItemDTO(npmAnggota, ak.getMahasiswa().getNama(), npmAnggota.equals(npmAktif));
    }
}
