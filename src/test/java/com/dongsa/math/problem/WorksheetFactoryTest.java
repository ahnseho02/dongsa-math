package com.dongsa.math.problem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("학습지 만들기")
class WorksheetFactoryTest {

    /** 슬롯이 남아 있거나 조사 표시가 안 바뀐 채로 나오면 안 된다. */
    private static final Pattern LEFTOVER = Pattern.compile("[{}]|«[^»]*$|undefined|null");

    @Autowired WorksheetFactory factory;

    private WorksheetSpec spec(int grade, int count, boolean carry, long seed) {
        return new WorksheetSpec(grade, factory.categoriesFor(grade), count, carry, seed);
    }

    @Test
    @DisplayName("같은 코드를 넣으면 똑같은 학습지가 다시 나온다")
    void sameSeedSameSheet() {
        WorksheetSpec s = spec(3, 10, true, 987654);
        Worksheet first = factory.build(s);
        Worksheet second = factory.build(s);

        assertThat(second.problems()).hasSize(10);
        assertThat(sentences(second)).isEqualTo(sentences(first));
        assertThat(answers(second)).isEqualTo(answers(first));
        assertThat(second.code()).isEqualTo(first.code());
    }

    @Test
    @DisplayName("시드가 다르면 다른 학습지가 나온다")
    void differentSeedDifferentSheet() {
        assertThat(sentences(factory.build(spec(3, 10, true, 1))))
                .isNotEqualTo(sentences(factory.build(spec(3, 10, true, 2))));
    }

    @ParameterizedTest(name = "초{0}학년")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("어느 학년으로 뽑아도 문장과 답이 성립한다")
    void everyGradeProducesSaneProblems(int grade) {
        List<String> broken = new ArrayList<>();

        for (boolean carry : new boolean[]{true, false}) {
            for (long seed = 0; seed < 40; seed++) {
                Worksheet sheet = factory.build(spec(grade, 10, carry, seed));
                assertThat(sheet.problems()).as("초%d 시드%d 문항 수", grade, seed).hasSize(10);

                String previous = null;
                for (GeneratedProblem p : sheet.problems()) {
                    if (LEFTOVER.matcher(p.sentence()).find()) {
                        broken.add(p.templateCode() + " 문장: " + p.sentence());
                    }
                    if (p.answer().isNegative()) {
                        broken.add(p.templateCode() + " 답이 음수: " + p.expression());
                    }
                    if (!p.operation().apply(p.numbers()).equalsValue(p.answer())) {
                        broken.add(p.templateCode() + " 답이 식과 안 맞음: " + p.expression());
                    }
                    // 정수끼리 나눴으면 답도 정수여야 한다 (초3 나눗셈은 나머지를 배우기 전이다).
                    // 소수·분수 나눗셈은 답이 정수가 아닌 것이 정상이다.
                    boolean allWhole = p.numbers().stream().allMatch(Num::isInteger);
                    if (p.operation() == Operation.DIVIDE && allWhole && !p.answer().isInteger()) {
                        broken.add(p.templateCode() + " 나머지가 남음: " + p.expression());
                    }
                    // 소수로 낸 문제인데 답이 분수로 나오면 소수로 안 떨어진 것이다 (예: 1 ÷ 3)
                    boolean anyDecimal = p.numbers().stream().anyMatch(n -> n.form() == Num.Form.DECIMAL);
                    boolean anyFraction = p.numbers().stream().anyMatch(n -> n.form() == Num.Form.FRACTION);
                    if (anyDecimal && !anyFraction && p.answerText().contains("/")) {
                        broken.add(p.templateCode() + " 소수로 안 떨어짐: " + p.expression()
                                   + " = " + p.answerText());
                    }
                    // 분수 문제의 답은 기약분수로 나와야 한다
                    if (p.answerText().contains("/")) {
                        String[] parts = p.answerText().split("/");
                        long n = Long.parseLong(parts[0]), d = Long.parseLong(parts[1]);
                        long g = java.math.BigInteger.valueOf(Math.abs(n))
                                .gcd(java.math.BigInteger.valueOf(d)).longValue();
                        if (g != 1) {
                            broken.add(p.templateCode() + " 기약분수가 아님: " + p.answerText());
                        }
                    }
                    if (p.cues().stream().noneMatch(c -> p.tappableWords().contains(c))) {
                        broken.add(p.templateCode() + " 단서가 문장에 없음: " + p.cues());
                    }
                    if (p.templateCode().equals(previous)) {
                        broken.add(p.templateCode() + " 같은 템플릿이 연달아 나옴");
                    }
                    previous = p.templateCode();
                }
            }
        }
        assertThat(broken).as("초%d학년에서 발견된 문제", grade).isEmpty();
    }

    @Test
    @DisplayName("학년에 안 맞는 문장은 나오지 않는다 — 초6에 '색 테이프 41273cm'는 없다")
    void templatesRespectGradeRange() {
        for (long seed = 0; seed < 60; seed++) {
            for (GeneratedProblem p : factory.build(spec(6, 10, true, seed)).problems()) {
                assertThat(p.templateCode())
                        .as("초6에 나오면 안 되는 템플릿")
                        .isNotIn("d3", "m1", "m3", "n1", "n4", "t1");
            }
        }
    }

    @Test
    @DisplayName("한 장 안에서는 템플릿을 한 바퀴 다 쓴 뒤에 다시 쓴다")
    void templatesAreSpreadOut() {
        Worksheet sheet = factory.build(spec(3, 10, true, 555));
        long distinct = sheet.problems().stream().map(GeneratedProblem::templateCode).distinct().count();
        assertThat(distinct).as("10문항이면 최소 8종류는 달라야 한다").isGreaterThanOrEqualTo(8);
    }

    @ParameterizedTest(name = "초{0}학년")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("한국어 띄어쓰기가 어긋난 문장이 없다")
    void koreanSpacingIsClean(int grade) {
        // 서술어와 조사는 앞말에 붙는다. "18864명 입니다" 처럼 뜬 문장이 나오면 안 된다.
        String[] mustNotAppear = {" 입니다", " 습니다", " 이고", " 을 ?", " 를 ?", "  "};
        List<String> bad = new ArrayList<>();

        for (long seed = 0; seed < 40; seed++) {
            for (GeneratedProblem p : factory.build(spec(grade, 10, true, seed)).problems()) {
                String text = p.plainSentence();
                for (String wrong : mustNotAppear) {
                    if (text.contains(wrong)) {
                        bad.add(p.templateCode() + " → \"" + wrong.strip() + "\" 앞이 떠 있음: " + text);
                    }
                }
            }
        }
        assertThat(bad).isEmpty();
    }

    @Test
    @DisplayName("유형을 하나만 고르면 그 유형만 나온다")
    void singleCategory() {
        WorksheetSpec s = new WorksheetSpec(3, List.of(ProblemCategory.SHARE), 6, true, 10);
        assertThat(factory.build(s).problems())
                .allMatch(p -> p.category() == ProblemCategory.SHARE)
                .allMatch(p -> p.operation() == Operation.DIVIDE);
    }

    @ParameterizedTest(name = "초{0}학년")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("어느 학년이든 낼 수 있는 유형이 넉넉하다")
    void everyGradeHasEnoughToOffer(int grade) {
        List<ProblemCategory> categories = factory.categoriesFor(grade);
        assertThat(categories).as("초%d 에 낼 수 있는 유형", grade).hasSizeGreaterThanOrEqualTo(3);

        // 한 유형만 골라도 10문항이 나와야 한다
        for (ProblemCategory category : categories) {
            Worksheet sheet = factory.build(new WorksheetSpec(grade, List.of(category), 10, true, 1));
            assertThat(sheet.problems()).as("초%d %s 한 유형만", grade, category.label()).hasSize(10);
        }
    }

    @Test
    @DisplayName("학년이 올라가면 다루는 수가 바뀐다 — 초4 소수, 초5 분수, 초6 비율")
    void higherGradesUseNewKindsOfNumbers() {
        assertThat(kindsAt(4)).as("초4 는 소수를 배운다").contains(Num.Form.DECIMAL);
        assertThat(kindsAt(5)).as("초5 는 분수를 배운다").contains(Num.Form.FRACTION);
        assertThat(kindsAt(6)).contains(Num.Form.DECIMAL, Num.Form.FRACTION);

        assertThat(kindsAt(3)).as("초3 까지는 정수만").containsExactly(Num.Form.INTEGER);
        assertThat(kindsAt(2)).containsExactly(Num.Form.INTEGER);

        assertThat(factory.categoriesFor(6)).as("비와 비율은 초6").contains(ProblemCategory.RATIO);
        assertThat(factory.categoriesFor(5)).doesNotContain(ProblemCategory.RATIO);
    }

    /** 그 학년 문제에 나오는 수의 종류를 모은다. */
    private java.util.Set<Num.Form> kindsAt(int grade) {
        java.util.Set<Num.Form> forms = new java.util.HashSet<>();
        for (long seed = 0; seed < 60; seed++) {
            factory.build(spec(grade, 10, true, seed)).problems()
                    .forEach(p -> p.numbers().forEach(n -> forms.add(n.form())));
        }
        return forms;
    }

    @Test
    @DisplayName("초1에는 곱셈·나눗셈 유형이 없다")
    void lowGradesHaveNoMultiplication() {
        assertThat(factory.categoriesFor(1))
                .doesNotContain(ProblemCategory.GROUP, ProblemCategory.SHARE)
                .contains(ProblemCategory.MERGE, ProblemCategory.INCREASE, ProblemCategory.DECREASE);
        assertThat(factory.categoriesFor(3)).contains(ProblemCategory.SHARE);
    }

    @Test
    @DisplayName("받아올림 '없음'을 고르면 쉬운 숫자만 나온다")
    void noCarryMeansEasyNumbers() {
        WorksheetSpec s = new WorksheetSpec(3, List.of(ProblemCategory.MERGE), 12, false, 77);
        for (GeneratedProblem p : factory.build(s).problems()) {
            assertThat(NumberFactory.hasCarry(p.numbers()))
                    .as("받아올림이 없어야 한다: %s", p.expression()).isFalse();
        }
    }

    private List<String> sentences(Worksheet w) {
        return w.problems().stream().map(GeneratedProblem::sentence).toList();
    }

    private List<String> answers(Worksheet w) {
        return w.problems().stream().map(GeneratedProblem::answerText).toList();
    }
}
