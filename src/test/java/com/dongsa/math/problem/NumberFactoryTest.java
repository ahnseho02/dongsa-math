package com.dongsa.math.problem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("제약 조건을 지키는 숫자 만들기")
class NumberFactoryTest {

    private static final int ROUNDS = 300;

    @ParameterizedTest(name = "초{0}학년")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("받아올림 '있음'으로 뽑으면 정말로 받아올림이 있다")
    void carryIsHonored(int grade) {
        GradeRange range = GradeRange.of(grade);
        Random rnd = new Random(grade);
        for (int i = 0; i < ROUNDS; i++) {
            List<Num> with = NumberFactory.generate(NumberPattern.ADD_PAIR, range, true, rnd);
            assertThat(NumberFactory.hasCarry(with)).as("초%d %s 받아올림 있음", grade, with).isTrue();

            List<Num> without = NumberFactory.generate(NumberPattern.ADD_PAIR, range, false, rnd);
            assertThat(NumberFactory.hasCarry(without)).as("초%d %s 받아올림 없음", grade, without).isFalse();
        }
    }

    @ParameterizedTest(name = "초{0}학년")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("뺄셈은 앞의 수가 항상 크고, 받아내림 조건도 지킨다")
    void subtractionNeverGoesNegative(int grade) {
        GradeRange range = GradeRange.of(grade);
        Random rnd = new Random(grade);
        for (int i = 0; i < ROUNDS; i++) {
            for (boolean borrow : new boolean[]{true, false}) {
                List<Num> n = NumberFactory.generate(NumberPattern.SUB_PAIR, range, borrow, rnd);
                assertThat(n.get(0)).as("%s 앞의 수가 더 커야 한다", n).isGreaterThan(n.get(1));

                // 한 자리 수끼리는 받아내림이 생길 수 없으므로 '있음'을 골라도 없는 문제가 나온다
                boolean expected = borrow && NumberFactory.borrowPossible(range);
                assertThat(NumberFactory.hasBorrow((int) n.get(0).numerator(), (int) n.get(1).numerator()))
                        .as("초%d %s 받아내림 %s", grade, n, expected).isEqualTo(expected);
            }
        }
    }

    @ParameterizedTest(name = "초{0}학년")
    @ValueSource(ints = {3, 4, 5, 6})
    @DisplayName("나눗셈은 항상 나누어떨어진다")
    void divisionHasNoRemainder(int grade) {
        GradeRange range = GradeRange.of(grade);
        Random rnd = new Random(grade);
        for (int i = 0; i < ROUNDS; i++) {
            List<Num> n = NumberFactory.generate(NumberPattern.DIV_PAIR, range, true, rnd);
            assertThat(n.get(0).numerator() % n.get(1).numerator()).as("%s 는 나누어떨어져야 한다", n).isZero();
            assertThat(n.get(0).dividedBy(n.get(1))).isGreaterThanOrEqualTo(Num.of(2));
        }
    }

    @Test
    @DisplayName("비교형은 차이값을 작게 뽑는다 — '145권보다 138권 더 많이'는 어색하다")
    void compareUsesSmallDifference() {
        GradeRange range = GradeRange.of(3);
        Random rnd = new Random(1);
        for (int i = 0; i < ROUNDS; i++) {
            List<Num> n = NumberFactory.generate(NumberPattern.ADD_SMALL_SECOND, range, true, rnd);
            assertThat(n.get(1)).isLessThanOrEqualTo(Num.of(range.max() / 5));
        }
    }

    @ParameterizedTest(name = "초{0}학년")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("세 수 덧셈도 받아올림 조건을 지킨다 — 무작위로는 거의 안 걸리는 조합이다")
    void tripleHonorsCarry(int grade) {
        GradeRange range = GradeRange.of(grade);
        Random rnd = new Random(7);
        for (int i = 0; i < ROUNDS; i++) {
            List<Num> without = NumberFactory.generate(NumberPattern.ADD_TRIPLE, range, false, rnd);
            assertThat(NumberFactory.hasCarry(without)).as("초%d %s", grade, without).isFalse();
            assertThat(without).allMatch(n -> n.compareTo(Num.of(range.min())) >= 0 && n.compareTo(Num.of(range.max())) <= 0,
                    "학년 범위 " + range.min() + "~" + range.max() + " 안");

            List<Num> with = NumberFactory.generate(NumberPattern.ADD_TRIPLE, range, true, rnd);
            assertThat(NumberFactory.hasCarry(with)).as("초%d %s", grade, with).isTrue();
        }
    }

    @ParameterizedTest(name = "초{0}학년")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("비교형도 받아올림 조건을 지킨다")
    void compareHonorsCarry(int grade) {
        GradeRange range = GradeRange.of(grade);
        Random rnd = new Random(11);
        for (int i = 0; i < ROUNDS; i++) {
            for (boolean carry : new boolean[]{true, false}) {
                List<Num> n = NumberFactory.generate(NumberPattern.ADD_SMALL_SECOND, range, carry, rnd);
                assertThat(NumberFactory.hasCarry(n)).as("초%d %s 받아올림 %s", grade, n, carry).isEqualTo(carry);
            }
        }
    }

    @Test
    @DisplayName("곱셈은 자릿수가 아니라 구구단 범위로 조절한다")
    void multiplicationStaysInReach() {
        Random rnd = new Random(3);
        for (int i = 0; i < ROUNDS; i++) {
            List<Num> low = NumberFactory.generate(NumberPattern.MUL_PAIR, GradeRange.of(2), true, rnd);
            assertThat(low.get(0)).isBetween(Num.of(2), Num.of(9));
            assertThat(low.get(1)).isBetween(Num.of(2), Num.of(9));

            List<Num> high = NumberFactory.generate(NumberPattern.MUL_PAIR, GradeRange.of(6), true, rnd);
            assertThat(high.get(1)).as("한 묶음의 개수는 구구단 안에 둔다").isBetween(Num.of(2), Num.of(9));
        }
    }
}
