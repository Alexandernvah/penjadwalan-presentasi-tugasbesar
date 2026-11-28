package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.PesertaKelas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PesertaKelasRepository extends JpaRepository<PesertaKelas, Integer> {

    @Query("""
        SELECT pk FROM PesertaKelas pk
        JOIN FETCH pk.mahasiswa m
        JOIN FETCH pk.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        WHERE m.npm IN :npms
        """)
    List<PesertaKelas> findByMahasiswaNpmInWithDetails(@Param("npms") Collection<String> npms);

    @Query("""
        SELECT pk FROM PesertaKelas pk
        JOIN FETCH pk.mahasiswa m
        JOIN FETCH pk.kelas k
        WHERE k.idKelas IN :kelasIds
        ORDER BY m.npm ASC
        """)
    List<PesertaKelas> findByKelasIdsWithMahasiswa(@Param("kelasIds") Collection<Integer> kelasIds);

    @Query("""
        SELECT pk FROM PesertaKelas pk
        JOIN FETCH pk.kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        LEFT JOIN FETCH k.penanggungJawab pj
        LEFT JOIN FETCH mkb.koordinator koord
        WHERE pk.mahasiswa.npm = :npm AND mkb.semesterAkademik = :semester
        """)
    List<PesertaKelas> findByNpmAndSemesterWithDetails(@Param("npm") String npm, @Param("semester") String semester);
}
