package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Kuliah;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface KuliahRepository extends JpaRepository<Kuliah, Integer> {

    List<Kuliah> findByKelas_IdKelasAndDosenPengajar_NomorIndukDosen(Integer idKelas, String nomorIndukDosen);

    List<Kuliah> findByKelas_IdKelas(Integer idKelas);

    @Query("""
        SELECT kul FROM Kuliah kul
        JOIN FETCH kul.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        JOIN FETCH kul.dosenPengajar d
        WHERE kul.hari = :hari
          AND d.nomorIndukDosen IN :dosenIds
        """)
    List<Kuliah> findByHariAndDosenIdsWithDetails(
        @Param("hari") Short hari,
        @Param("dosenIds") Collection<String> dosenIds
    );

    @Query("""
        SELECT kul FROM Kuliah kul
        JOIN FETCH kul.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        JOIN FETCH kul.dosenPengajar d
        WHERE kul.hari = :hari
          AND k.idKelas IN :kelasIds
        """)
    List<Kuliah> findByHariAndKelasIdsWithDetails(
        @Param("hari") Short hari,
        @Param("kelasIds") Collection<Integer> kelasIds
    );

    @Query("""
        SELECT kul FROM Kuliah kul
        JOIN FETCH kul.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        JOIN FETCH kul.dosenPengajar d
        WHERE kul.hari = :hari
          AND LOWER(kul.ruangan) = LOWER(:ruangan)
        """)
    List<Kuliah> findByHariAndRuanganWithDetails(
        @Param("hari") Short hari,
        @Param("ruangan") String ruangan
    );

    @Query("SELECT DISTINCT kul.ruangan FROM Kuliah kul WHERE kul.ruangan IS NOT NULL")
    List<String> findDistinctRuangan();

    @Query("""
        SELECT kul FROM Kuliah kul
        JOIN FETCH kul.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        JOIN FETCH kul.dosenPengajar d
        WHERE d.nomorIndukDosen IN :dosenIds
        """)
    List<Kuliah> findByDosenIdsWithDetails(@Param("dosenIds") Collection<String> dosenIds);

    @Query("""
        SELECT kul FROM Kuliah kul
        JOIN FETCH kul.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        JOIN FETCH kul.dosenPengajar d
        WHERE k.idKelas IN :kelasIds
        """)
    List<Kuliah> findByKelasIdsWithDetails(@Param("kelasIds") Collection<Integer> kelasIds);
}
