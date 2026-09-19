package com.dongsa.math.repository;

import com.dongsa.math.domain.GameRound;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GameRoundRepository extends JpaRepository<GameRound, Long> {

    Optional<GameRound> findByIdAndStudentId(Long id, Long studentId);

    /**
     * 학원 순위표. 아이마다 **가장 잘한 한 판**만 센다.
     * 많이 맞힌 순, 같으면 빨리 끝낸 순.
     */
    @Query("""
           select g.student.id, g.student.name, g.student.grade, max(g.solved), min(g.elapsedMs), count(g)
           from GameRound g
           where g.academy.id = :academyId and g.finishedAt is not null
           group by g.student.id, g.student.name, g.student.grade
           order by max(g.solved) desc, min(g.elapsedMs) asc
           """)
    List<Object[]> ranking(Long academyId, Pageable pageable);

    @Query("""
           select count(distinct g.student.id) from GameRound g
           where g.academy.id = :academyId and g.finishedAt is not null
           """)
    long countPlayers(Long academyId);

    @Query("""
           select coalesce(max(g.solved), 0) from GameRound g
           where g.student.id = :studentId and g.finishedAt is not null
           """)
    int bestOf(Long studentId);

    @Query("""
           select count(g) from GameRound g
           where g.student.id = :studentId and g.finishedAt is not null
           """)
    long playCountOf(Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GameRound g where g.student.id = :studentId")
    int deleteAllForStudent(Long studentId);
}
