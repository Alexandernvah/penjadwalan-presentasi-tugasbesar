package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Mahasiswa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MahasiswaRepository extends JpaRepository<Mahasiswa, String> {

    Optional<Mahasiswa> findByNpm(String npm);

    Optional<Mahasiswa> findByAkun_IdAkun(Integer idAkun);

    Optional<Mahasiswa> findByAkun_EmailIgnoreCase(String email);
}
