package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Dosen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DosenRepository extends JpaRepository<Dosen, String> {

    Optional<Dosen> findByNomorIndukDosen(String nomorIndukDosen);

    Optional<Dosen> findByAkun_Email(String email);
}
