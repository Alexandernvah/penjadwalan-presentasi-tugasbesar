package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Kelompok;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KelompokRepository extends JpaRepository<Kelompok, Integer> {
    List<Kelompok> findByKelas_IdKelasOrderByNoKelompokAsc(Integer idKelas);
}
