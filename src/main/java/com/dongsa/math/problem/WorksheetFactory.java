package com.dongsa.math.problem;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 학습지 만들기.
 *
 * 문제를 통째로 저장하지 않고 시드만 저장한다. 같은 조건 + 같은 시드면 항상 같은 문제가 나오므로
 * "지난주에 낸 그 시험지 다시" 가 된다. 대신 재현이 성립하려면 다음 두 가지를 지켜야 한다.
 *   1. 템플릿 목록의 순서가 항상 같을 것 (그래서 code 로 정렬해서 가져온다)
 *   2. 난수를 쓰는 순서가 항상 같을 것
 */
@Service
public class WorksheetFactory {

    private static final int MAX_TRIES_MULTIPLIER = 20;

    private final ProblemTemplateRepository templates;
    private final ProblemGenerator generator;

    public WorksheetFactory(ProblemTemplateRepository templates, ProblemGenerator generator) {
        this.templates = templates;
        this.generator = generator;
    }

    @Transactional(readOnly = true)
    public Worksheet build(WorksheetSpec spec) {
        List<ProblemTemplate> pool = templates.findUsable(spec.categories(), spec.grade());
        if (pool.isEmpty()) {
            throw new ApiException(ErrorCode.NO_TEMPLATE_FOR_GRADE);
        }

        GradeRange grade = GradeRange.of(spec.grade());
        Random rnd = new Random(spec.seed());

        List<GeneratedProblem> problems = new ArrayList<>(spec.count());
        List<ProblemTemplate> unused = new ArrayList<>();
        String previousCode = null;
        int guard = spec.count() * MAX_TRIES_MULTIPLIER;

        while (problems.size() < spec.count() && guard-- > 0) {
            // 있는 템플릿을 한 바퀴 다 쓰고 나서 다시 채운다 — 한 장 안에서 같은 문제가 반복되지 않게
            if (unused.isEmpty()) {
                unused.addAll(pool);
            }
            ProblemTemplate template = unused.remove(rnd.nextInt(unused.size()));
            if (template.getCode().equals(previousCode) && pool.size() > 1) {
                continue;
            }
            problems.add(generator.generate(template, grade, spec.carry(), rnd));
            previousCode = template.getCode();
        }

        return new Worksheet(spec.code(), spec, List.copyOf(problems));
    }

    /** 그 학년에 낼 수 있는 유형. 화면에서 고를 수 없는 유형을 보여 주지 않기 위해 쓴다. */
    @Transactional(readOnly = true)
    public List<ProblemCategory> categoriesFor(int grade) {
        return templates.findCategoriesForGrade(grade).stream()
                .sorted()
                .toList();
    }
}
