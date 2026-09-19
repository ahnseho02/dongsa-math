package com.dongsa.math.repository;

import com.dongsa.math.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByAcademyIdOrderByNameAsc(Long academyId);

    List<Student> findByAcademyIdAndClassroomIdOrderByNameAsc(Long academyId, Long classroomId);

    /** 학생 로그인: 학원 안에서 이름이 같은 학생을 모두 찾아 PIN 으로 가린다. (동명이인 대비) */
    List<Student> findByAcademyIdAndNameAndActiveTrue(Long academyId, String name);

    Optional<Student> findByIdAndAcademyId(Long id, Long academyId);

    long countByAcademyId(Long academyId);
}
