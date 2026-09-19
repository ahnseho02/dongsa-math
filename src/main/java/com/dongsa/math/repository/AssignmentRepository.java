package com.dongsa.math.repository;

import com.dongsa.math.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByAcademyIdOrderByIdDesc(Long academyId);

    Optional<Assignment> findByIdAndAcademyId(Long id, Long academyId);

    /** 반을 지워도 그 반에 낸 과제와 기록은 남는다. 반 연결만 끊는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Assignment a set a.classroom = null where a.classroom.id = :classroomId")
    int detachFromClassroom(Long classroomId);
}
