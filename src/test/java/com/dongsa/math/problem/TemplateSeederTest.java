package com.dongsa.math.problem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("문제 템플릿 동기화")
class TemplateSeederTest {

    @Autowired TemplateSeeder seeder;
    @Autowired ProblemTemplateRepository templates;

    @Test
    @DisplayName("이미 맞춰진 뒤에 다시 돌리면 아무것도 바뀌지 않는다")
    void secondRunChangesNothing() throws Exception {
        // 앱이 뜰 때 이미 한 번 돌았다
        TemplateSeeder.SyncResult again = seeder.sync();

        assertThat(again.total()).isEqualTo(templates.count());
        assertThat(again.added()).as("새로 추가된 것이 있으면 안 된다").isZero();
        assertThat(again.updated()).as("바뀐 것이 없는데 수정으로 잡히면 안 된다").isZero();
    }

    @Test
    @DisplayName("문장을 고치면 다음 실행에서 DB 가 따라온다")
    void changedPatternIsPickedUp() {
        ProblemTemplate template = templates.findByCode("m1").orElseThrow();
        String original = template.getPattern();

        ProblemTemplate incoming = new ProblemTemplate(
                template.getCode(), template.getCategory(), template.getOperation(),
                template.getNumberPattern(), "{name}{은/는} 바뀐 문장입니다.", template.getUnit(),
                template.getMinGrade(), template.getMaxGrade(), template.getCues(),
                template.getSecondScale(), template.isNeedsTwoNames());

        assertThat(template.syncFrom(incoming)).isTrue();
        assertThat(template.getPattern()).isEqualTo("{name}{은/는} 바뀐 문장입니다.");

        // 같은 내용을 한 번 더 넣으면 변화 없음
        assertThat(template.syncFrom(incoming)).isFalse();

        template.syncFrom(new ProblemTemplate(
                template.getCode(), template.getCategory(), template.getOperation(),
                template.getNumberPattern(), original, template.getUnit(),
                template.getMinGrade(), template.getMaxGrade(), template.getCues(),
                template.getSecondScale(), template.isNeedsTwoNames()));
    }
}
