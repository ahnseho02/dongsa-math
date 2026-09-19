package com.dongsa.math.repository;

import com.dongsa.math.domain.Academy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademyRepository extends JpaRepository<Academy, Long> {
    Optional<Academy> findByCode(String code);
    boolean existsByCode(String code);
}
