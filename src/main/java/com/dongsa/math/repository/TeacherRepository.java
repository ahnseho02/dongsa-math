package com.dongsa.math.repository;

import com.dongsa.math.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Teacher> findByAcademyIdOrderByIdAsc(Long academyId);
    Optional<Teacher> findByIdAndAcademyId(Long id, Long academyId);
}
