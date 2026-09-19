package com.dongsa.math.repository;

import com.dongsa.math.domain.Submission;
import com.dongsa.math.problem.SolveStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 제출 기록.
 *
 * 리포트는 전부 여기서 group by 로 뽑는다. 학생이 30명이든 300명이든 쿼리 수는 그대로다.
 * 돌려주는 Object[] 는 {묶은 값, 맞힌 수, 전체 수} 순서다.
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByStudentAssignmentIdOrderByProblemNoAscStepAsc(Long studentAssignmentId);

    Optional<Submission> findByStudentAssignmentIdAndProblemNoAndStep(
            Long studentAssignmentId, int problemNo, SolveStep step);

    List<Submission> findByStudentAssignmentIdAndProblemNo(Long studentAssignmentId, int problemNo);

    long countByStudentAssignmentId(Long studentAssignmentId);

    long countByStudentAssignmentIdAndCorrectTrue(Long studentAssignmentId);

    // ── 과제 하나에 대한 집계 ──

    @Query("""
           select s.step, sum(case when s.correct then 1 else 0 end), count(s)
           from Submission s
           where s.studentAssignment.assignment.id = :assignmentId
           group by s.step
           """)
    List<Object[]> countByStepForAssignment(Long assignmentId);

    @Query("""
           select s.category, sum(case when s.correct then 1 else 0 end), count(s)
           from Submission s
           where s.studentAssignment.assignment.id = :assignmentId
           group by s.category
           """)
    List<Object[]> countByCategoryForAssignment(Long assignmentId);

    @Query("""
           select s.studentAssignment.id, s.step, sum(case when s.correct then 1 else 0 end), count(s)
           from Submission s
           where s.studentAssignment.assignment.id = :assignmentId
           group by s.studentAssignment.id, s.step
           """)
    List<Object[]> countByStudentAssignmentAndStep(Long assignmentId);

    // ── 학생 한 명의 누적 집계 (모든 과제를 통틀어) ──

    @Query("""
           select s.step, sum(case when s.correct then 1 else 0 end), count(s)
           from Submission s
           where s.studentAssignment.student.id = :studentId
           group by s.step
           """)
    List<Object[]> countByStepForStudent(Long studentId);

    @Query("""
           select s.category, sum(case when s.correct then 1 else 0 end), count(s)
           from Submission s
           where s.studentAssignment.student.id = :studentId
           group by s.category
           """)
    List<Object[]> countByCategoryForStudent(Long studentId);

    @Query("""
           select s.studentAssignment.id, sum(case when s.correct then 1 else 0 end), count(s)
           from Submission s
           where s.studentAssignment.student.id = :studentId
           group by s.studentAssignment.id
           """)
    List<Object[]> countByStudentAssignmentForStudent(Long studentId);

    // ── 학원 전체 학생을 한눈에 ──

    @Query("""
           select s.studentAssignment.student.id, s.step, sum(case when s.correct then 1 else 0 end), count(s)
           from Submission s
           where s.studentAssignment.student.academy.id = :academyId
           group by s.studentAssignment.student.id, s.step
           """)
    List<Object[]> countByStudentAndStepInAcademy(Long academyId);

    /**
     * 학생을 완전히 삭제할 때 기록부터 지운다.
     * 외래키는 일부러 제한(restrict)으로 두었다 — 실수로 지울 때 조용히 성적이 날아가지 않게.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           delete from Submission s
           where s.studentAssignment.id in (select sa.id from StudentAssignment sa where sa.student.id = :studentId)
           """)
    int deleteAllForStudent(Long studentId);
}
