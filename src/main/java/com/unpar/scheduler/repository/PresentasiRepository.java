package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Presentasi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresentasiRepository extends JpaRepository<Presentasi, Integer> {

    Optional<Presentasi> findByKelompok_IdKelompok(Integer idKelompok);

    @Modifying
    @Query("DELETE FROM Presentasi p WHERE p.kelompok.idKelompok = :idKelompok")
    void deleteByKelompokId(@Param("idKelompok") Integer idKelompok);
}
