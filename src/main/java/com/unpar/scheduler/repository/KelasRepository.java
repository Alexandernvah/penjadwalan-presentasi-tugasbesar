package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Kelas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface KelasRepository extends JpaRepository<Kelas, Integer> {

    @Query("SELECT DISTINCT k FROM Kelas k JOIN k.mataKuliahBuka mkb JOIN Kuliah kul ON kul.kelas.idKelas = k.idKelas WHERE kul.dosenPengajar.nomorIndukDosen = :nidn AND mkb.semesterAkademik = :semester")
    List<Kelas> findClassesByLecturerAndSemester(@Param("nidn") String nidn, @Param("semester") String semester);

    @Query("""
        SELECT k FROM Kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        LEFT JOIN FETCH k.penanggungJawab pj
        LEFT JOIN FETCH mkb.koordinator koord
        WHERE k.idKelas = :idKelas
        """)
    Optional<Kelas> findByIdWithDetails(@Param("idKelas") Integer idKelas);

    @Query("""
        SELECT MAX(mkb.semesterAkademik) FROM Kelas k
        JOIN k.mataKuliahBuka mkb
        JOIN Kuliah kul ON kul.kelas.idKelas = k.idKelas
        WHERE kul.dosenPengajar.nomorIndukDosen = :nidn
        """)
    Optional<String> findLatestSemesterByLecturer(@Param("nidn") String nidn);

    @Query("SELECT MAX(mkb.semesterAkademik) FROM MataKuliahBuka mkb")
    Optional<String> findLatestSemesterOverall();

    @Query("""
        SELECT k FROM Kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        WHERE mkb.semesterAkademik = :semester
        ORDER BY k.kodeKelas ASC
        """)
    List<Kelas> findBySemester(@Param("semester") String semester);

    @Query("""
        SELECT k FROM Kelas k
        JOIN FETCH k.mataKuliahBuka mkb
        JOIN FETCH mkb.matakuliah mk
        LEFT JOIN FETCH k.penanggungJawab pj
        WHERE mkb.semesterAkademik = :semester
        ORDER BY mk.nama ASC, k.kodeKelas ASC
        """)
    List<Kelas> findBySemesterWithDetails(@Param("semester") String semester);
}
