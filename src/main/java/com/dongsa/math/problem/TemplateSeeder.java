package com.dongsa.math.problem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * 문제 템플릿을 JSON 에서 읽어 DB 와 맞춘다.
 * JSON 이 문장의 원본이다 — 파일을 고치고 다시 띄우면 DB 가 따라온다.
 * (관리 화면이 생기면 원본을 DB 로 옮기고 이 클래스는 최초 1회만 돌게 바꾼다.)
 */
@Component
public class TemplateSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TemplateSeeder.class);
    private static final String PATH = "data/problem-templates.json";

    private final ProblemTemplateRepository templates;
    private final ObjectMapper mapper;

    public TemplateSeeder(ProblemTemplateRepository templates, ObjectMapper mapper) {
        this.templates = templates;
        this.mapper = mapper;
    }

    /** 몇 개가 새로 들어갔고 몇 개가 바뀌었는지. 두 번째 실행은 0, 0 이어야 한다. */
    public record SyncResult(int total, int added, int updated) {}

    @Override
    public void run(ApplicationArguments args) throws Exception {
        SyncResult result = sync();
        log.info("문제 템플릿 {}개 · 새로 추가 {}개 · 수정 {}개",
                result.total(), result.added(), result.updated());
    }

    @Transactional
    public SyncResult sync() throws Exception {
        List<Row> rows;
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            rows = mapper.readValue(in, mapper.getTypeFactory().constructCollectionType(List.class, Row.class));
        }

        int added = 0;
        int updated = 0;
        for (Row row : rows) {
            ProblemTemplate incoming = row.toEntity();
            ProblemTemplate existing = templates.findByCode(row.code()).orElse(null);
            if (existing == null) {
                templates.save(incoming);
                added++;
            } else if (existing.syncFrom(incoming)) {
                updated++;
            }
        }
        return new SyncResult(rows.size(), added, updated);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Row(String code, ProblemCategory category, Operation operation, NumberPattern numberPattern,
               String pattern, String unit, Integer minGrade, Integer maxGrade,
               List<String> cues, Integer secondScale, Boolean needsTwoNames) {

        ProblemTemplate toEntity() {
            return new ProblemTemplate(
                    code, category, operation, numberPattern, pattern, unit,
                    minGrade == null ? 1 : minGrade,
                    maxGrade == null ? 6 : maxGrade,
                    cues == null ? List.of() : cues,
                    secondScale == null ? 1 : secondScale,
                    Boolean.TRUE.equals(needsTwoNames));
        }
    }
}
