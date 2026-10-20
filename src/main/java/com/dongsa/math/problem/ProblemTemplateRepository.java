package com.dongsa.math.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProblemTemplateRepository extends JpaRepository<ProblemTemplate, Long> {

    /**
     * 고른 유형 중에서 그 학년에 어울리는 것만. 순서를 고정해야 시드 재현이 성립한다.
     * challenge 가 꺼져 있으면 도전 문제(hard)는 빠진다.
     */
    @Query("""
           select t from ProblemTemplate t
           where t.category in :categories
             and t.minGrade <= :grade
             and t.maxGrade >= :grade
             and (:challenge = true or t.hard = false)
           order by t.code asc
           """)
    List<ProblemTemplate> findUsable(Collection<ProblemCategory> categories, int grade, boolean challenge);

    @Query("""
           select distinct t.category from ProblemTemplate t
           where t.minGrade <= :grade and t.maxGrade >= :grade
             and (:challenge = true or t.hard = false)
           """)
    List<ProblemCategory> findCategoriesForGrade(int grade, boolean challenge);

    Optional<ProblemTemplate> findByCode(String code);
}
