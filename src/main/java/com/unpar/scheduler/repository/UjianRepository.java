package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Ujian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface UjianRepository extends JpaRepository<Ujian, Integer> {

    @Query("""
        SELECT u FROM Ujian u
        JOIN FETCH u.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE u.tanggal = :tanggal
          AND LOWER(u.ruangan) = LOWER(:ruangan)
        """)
    List<Ujian> findByTanggalAndRuanganWithDetails(
        @Param("tanggal") LocalDate tanggal,
        @Param("ruangan") String ruangan
    );

    @Query("""
        SELECT u FROM Ujian u
        JOIN FETCH u.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE u.tanggal = :tanggal
          AND mkb.idMatkulBuka IN :matkulBukaIds
        """)
    List<Ujian> findByTanggalAndMatkulBukaIds(
        @Param("tanggal") LocalDate tanggal,
        @Param("matkulBukaIds") Collection<Integer> matkulBukaIds
    );

    @Query("SELECT DISTINCT u.ruangan FROM Ujian u WHERE u.ruangan IS NOT NULL")
    List<String> findDistinctRuangan();

    @Query("""
        SELECT u FROM Ujian u
        JOIN FETCH u.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE u.tanggal BETWEEN :startDate AND :endDate
          AND mkb.idMatkulBuka IN :matkulBukaIds
        """)
    List<Ujian> findByTanggalBetweenAndMatkulBukaIds(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("matkulBukaIds") Collection<Integer> matkulBukaIds
    );
}
