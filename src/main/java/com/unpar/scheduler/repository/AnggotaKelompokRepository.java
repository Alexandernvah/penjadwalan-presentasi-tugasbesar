package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.AnggotaKelompok;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AnggotaKelompokRepository extends JpaRepository<AnggotaKelompok, Integer> {
    List<AnggotaKelompok> findByKelompok_IdKelompok(Integer idKelompok);
    List<AnggotaKelompok> findByKelas_IdKelas(Integer idKelas);
    void deleteByKelompok_IdKelompok(Integer idKelompok);

    boolean existsByMahasiswa_NpmAndKelas_IdKelas(String npm, Integer idKelas);

    @Query("""
        SELECT ak FROM AnggotaKelompok ak
        JOIN FETCH ak.mahasiswa m
        JOIN FETCH ak.kelompok kel
        WHERE ak.kelas.idKelas = :idKelas
        """)
    List<AnggotaKelompok> findByKelasIdWithMahasiswaAndKelompok(@Param("idKelas") Integer idKelas);

    @Query("""
        SELECT ak FROM AnggotaKelompok ak
        JOIN FETCH ak.mahasiswa m
        JOIN FETCH ak.kelompok kel
        JOIN FETCH ak.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE m.npm IN :npms
        """)
    List<AnggotaKelompok> findByMahasiswaNpmInWithDetails(@Param("npms") Collection<String> npms);

    @Query("""
        SELECT ak FROM AnggotaKelompok ak
        JOIN FETCH ak.mahasiswa m
        JOIN FETCH ak.kelompok kel
        WHERE kel.idKelompok IN :kelompokIds
        ORDER BY m.npm ASC
        """)
    List<AnggotaKelompok> findByKelompokIdsWithMahasiswa(@Param("kelompokIds") Collection<Integer> kelompokIds);
}
