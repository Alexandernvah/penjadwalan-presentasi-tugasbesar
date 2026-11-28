package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Kelompok;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KelompokRepository extends JpaRepository<Kelompok, Integer> {
    List<Kelompok> findByKelas_IdKelasOrderByNoKelompokAsc(Integer idKelas);

    // Mengunci baris kelompok selama gabung/keluar agar kuota anggota tidak terlampaui saat concurrent request.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM Kelompok k JOIN FETCH k.kelas WHERE k.idKelompok = :idKelompok")
    Optional<Kelompok> findByIdForUpdate(@Param("idKelompok") Integer idKelompok);
}
