package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Mahasiswa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MahasiswaRepository extends JpaRepository<Mahasiswa, String> {

    Optional<Mahasiswa> findByNpm(String npm);

    Optional<Mahasiswa> findByAkun_IdAkun(Integer idAkun);

    Optional<Mahasiswa> findByAkun_EmailIgnoreCase(String email);

    // Mengunci baris mahasiswa agar dua submit input mata kuliah bersamaan diproses berurutan.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mahasiswa m WHERE m.npm = :npm")
    Optional<Mahasiswa> findByNpmForUpdate(@Param("npm") String npm);
}
