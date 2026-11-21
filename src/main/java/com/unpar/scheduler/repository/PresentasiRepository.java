package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Presentasi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresentasiRepository extends JpaRepository<Presentasi, Integer> {

    Optional<Presentasi> findByKelompok_IdKelompok(Integer idKelompok);

    @Modifying
    @Query("DELETE FROM Presentasi p WHERE p.kelompok.idKelompok = :idKelompok")
    void deleteByKelompokId(@Param("idKelompok") Integer idKelompok);

    @Query("""
        SELECT p FROM Presentasi p
        JOIN FETCH p.kelompok kel
        JOIN FETCH kel.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE k.idKelas = :idKelas
        """)
    List<Presentasi> findByKelasIdWithDetails(@Param("idKelas") Integer idKelas);

    @Query("""
        SELECT p FROM Presentasi p
        JOIN FETCH p.kelompok kel
        JOIN FETCH kel.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE p.tanggal = :tanggal
        """)
    List<Presentasi> findByTanggalWithDetails(@Param("tanggal") LocalDate tanggal);

    @Query("""
        SELECT p FROM Presentasi p
        JOIN FETCH p.kelompok kel
        JOIN FETCH kel.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE p.tanggal = :tanggal
          AND LOWER(p.ruangan) = LOWER(:ruangan)
        """)
    List<Presentasi> findByTanggalAndRuanganWithDetails(
        @Param("tanggal") LocalDate tanggal,
        @Param("ruangan") String ruangan
    );

    @Query("""
        SELECT p FROM Presentasi p
        JOIN FETCH p.kelompok kel
        JOIN FETCH kel.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE p.tanggal = :tanggal
          AND kel.idKelompok IN :kelompokIds
        """)
    List<Presentasi> findByTanggalAndKelompokIdsWithDetails(
        @Param("tanggal") LocalDate tanggal,
        @Param("kelompokIds") Collection<Integer> kelompokIds
    );

    @Query("SELECT DISTINCT p.ruangan FROM Presentasi p WHERE p.ruangan IS NOT NULL")
    List<String> findDistinctRuangan();

    @Query("""
        SELECT p FROM Presentasi p
        JOIN FETCH p.kelompok kel
        JOIN FETCH kel.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE p.tanggal BETWEEN :startDate AND :endDate
        """)
    List<Presentasi> findByTanggalBetweenWithDetails(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
