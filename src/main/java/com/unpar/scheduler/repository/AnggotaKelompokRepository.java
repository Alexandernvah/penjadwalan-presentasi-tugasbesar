package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.AnggotaKelompok;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnggotaKelompokRepository extends JpaRepository<AnggotaKelompok, Integer> {
    List<AnggotaKelompok> findByKelompok_IdKelompok(Integer idKelompok);
    List<AnggotaKelompok> findByKelas_IdKelas(Integer idKelas);
}
