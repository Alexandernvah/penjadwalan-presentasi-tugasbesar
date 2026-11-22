package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.PengawasUjian;
import com.unpar.scheduler.model.entity.PengawasUjianId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface PengawasUjianRepository extends JpaRepository<PengawasUjian, PengawasUjianId> {

    @Query("""
        SELECT pu FROM PengawasUjian pu
        JOIN FETCH pu.dosen d
        JOIN FETCH pu.ujian u
        JOIN FETCH u.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE u.tanggal = :tanggal
          AND d.nomorIndukDosen IN :dosenIds
        """)
    List<PengawasUjian> findByTanggalAndDosenIdsWithDetails(
        @Param("tanggal") LocalDate tanggal,
        @Param("dosenIds") Collection<String> dosenIds
    );

    @Query("""
        SELECT pu FROM PengawasUjian pu
        JOIN FETCH pu.dosen d
        JOIN FETCH pu.ujian u
        JOIN FETCH u.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE u.tanggal BETWEEN :startDate AND :endDate
          AND d.nomorIndukDosen IN :dosenIds
        """)
    List<PengawasUjian> findByTanggalBetweenAndDosenIdsWithDetails(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("dosenIds") Collection<String> dosenIds
    );

    @Query("""
        SELECT pu FROM PengawasUjian pu
        JOIN FETCH pu.dosen d
        JOIN FETCH pu.ujian u
        JOIN FETCH u.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE d.nomorIndukDosen = :nidn
          AND mkb.semesterAkademik = :semester
        ORDER BY u.tanggal ASC, u.jamMulai ASC
        """)
    List<PengawasUjian> findByDosenAndSemesterWithDetails(
        @Param("nidn") String nidn,
        @Param("semester") String semester
    );

    @Query("""
        SELECT pu FROM PengawasUjian pu
        JOIN FETCH pu.dosen d
        JOIN FETCH pu.ujian u
        JOIN FETCH u.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE mkb.semesterAkademik = :semester
        ORDER BY u.tanggal ASC, u.jamMulai ASC, d.nama ASC
        """)
    List<PengawasUjian> findBySemesterWithDetails(@Param("semester") String semester);

    @Query("""
        SELECT pu FROM PengawasUjian pu
        JOIN FETCH pu.dosen d
        JOIN FETCH pu.ujian u
        WHERE u.idUjian = :idUjian
        ORDER BY d.nama ASC
        """)
    List<PengawasUjian> findByUjianIdWithDosen(@Param("idUjian") Integer idUjian);
}
