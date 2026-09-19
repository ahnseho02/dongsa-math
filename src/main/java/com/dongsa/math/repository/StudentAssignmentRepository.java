package com.dongsa.math.repository;

import com.dongsa.math.domain.StudentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentAssignmentRepository extends JpaRepository<StudentAssignment, Long> {

    /** 선생님이 보는 반 현황. 학생까지 같이 가져와 N+1 을 막는다. */
    @Query("""
           select sa from StudentAssignment sa
             join fetch sa.student
           where sa.assignment.id = :assignmentId
           order by sa.student.name asc
           """)
    List<StudentAssignment> findWithStudentByAssignmentId(Long assignmentId);

    /** 아이가 보는 내 과제 목록. */
    @Query("""
           select sa from StudentAssignment sa
             join fetch sa.assignment a
           where sa.student.id = :studentId
           order by sa.id desc
           """)
    List<StudentAssignment> findWithAssignmentByStudentId(Long studentId);

    Optional<StudentAssignment> findByIdAndStudentId(Long id, Long studentId);

    long countByAssignmentId(Long assignmentId);

    long countByAssignmentIdAndCompletedAtIsNotNull(Long assignmentId);

    long countByStudentId(Long studentId);

    long countByStudentIdAndCompletedAtIsNotNull(Long studentId);

    long countByStudentIdAndStartedAtIsNotNull(Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StudentAssignment sa where sa.student.id = :studentId")
    int deleteAllForStudent(Long studentId);
}
