package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Kuliah;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KuliahRepository extends JpaRepository<Kuliah, Integer> {

    List<Kuliah> findByKelas_IdKelasAndDosenPengajar_NomorIndukDosen(Integer idKelas, String nomorIndukDosen);
}
