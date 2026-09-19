package com.dongsa.math.service;

import com.dongsa.math.problem.*;
import com.dongsa.math.web.dto.WorksheetDtos.*;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class WorksheetService {

    private final WorksheetFactory factory;
    private final SecureRandom random = new SecureRandom();

    public WorksheetService(WorksheetFactory factory) {
        this.factory = factory;
    }

    public WorksheetResponse generate(GenerateRequest req) {
        Long fromCode = ReadableCode.decode(req.code());
        long seed = fromCode != null ? fromCode : newSeed();

        WorksheetSpec spec = new WorksheetSpec(
                req.grade(), req.categories(), req.countOrDefault(), req.carryOrDefault(), seed);

        return WorksheetResponse.of(factory.build(spec));
    }

    /** 화면에서 학년을 고르면 그 학년에 낼 수 있는 유형만 보여 주기 위한 정보. */
    public GradeOptions options(int grade) {
        GradeRange range = GradeRange.of(grade);
        List<CategoryOption> categories = factory.categoriesFor(grade).stream()
                .map(c -> new CategoryOption(c.name(), c.label(), operationHint(c)))
                .toList();
        return new GradeOptions(grade, range.label(), categories);
    }

    /** 유형별 대표 연산 기호. 비교하기는 묻는 내용에 따라 갈려서 둘 다 적는다. */
    private String operationHint(ProblemCategory category) {
        return switch (category) {
            case MERGE, INCREASE, MULTI -> Operation.ADD.sign();
            case DECREASE -> Operation.SUBTRACT.sign();
            case COMPARE -> Operation.ADD.sign() + Operation.SUBTRACT.sign();
            case GROUP -> Operation.MULTIPLY.sign();
            case SHARE -> Operation.DIVIDE.sign();
        };
    }

    private long newSeed() {
        return Math.floorMod(random.nextLong(), ReadableCode.MAX_SEED);
    }
}
