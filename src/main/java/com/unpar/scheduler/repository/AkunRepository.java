package com.unpar.scheduler.repository;

import com.unpar.scheduler.model.entity.Akun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AkunRepository extends JpaRepository<Akun, Integer> {

    Optional<Akun> findByEmailIgnoreCase(String email);
}
