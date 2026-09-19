package com.dongsa.math.repository;

import com.dongsa.math.domain.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findByAcademyIdOrderByIdAsc(Long academyId);
    Optional<Classroom> findByIdAndAcademyId(Long id, Long academyId);
    boolean existsByAcademyIdAndName(Long academyId, String name);
}
