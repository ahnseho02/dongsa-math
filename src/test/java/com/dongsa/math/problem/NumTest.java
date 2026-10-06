package com.dongsa.math.problem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("문제에 나오는 수")
class NumTest {

    @Test
    @DisplayName("소수를 더해도 오차가 생기지 않는다 — double 이면 0.30000000000000004 가 된다")
    void decimalsAreExact() {
        Num result = Num.decimal(1, 1).plus(Num.decimal(2, 1));   // 0.1 + 0.2
        assertThat(result.text()).isEqualTo("0.3");
        assertThat(result.equalsValue(Num.decimal(3, 1))).isTrue();
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);   // 참고: double 로는 이렇게 된다
    }

    @Test
    @DisplayName("분수는 기약분수로 줄여서 들고 있다")
    void fractionsAreReduced() {
        assertThat(Num.fraction(2, 4).text()).isEqualTo("1/2");
        assertThat(Num.fraction(6, 8).text()).isEqualTo("3/4");
        assertThat(Num.fraction(2, 4).equalsValue(Num.fraction(1, 2))).isTrue();
    }

    @Test
    @DisplayName("분수끼리 계산해도 정확하다")
    void fractionArithmetic() {
        assertThat(Num.fraction(1, 3).plus(Num.fraction(1, 6)).text()).isEqualTo("1/2");
        assertThat(Num.fraction(3, 4).minus(Num.fraction(1, 4)).text()).isEqualTo("1/2");
        assertThat(Num.fraction(2, 3).times(Num.fraction(3, 4)).text()).isEqualTo("1/2");
        assertThat(Num.fraction(1, 2).dividedBy(Num.fraction(1, 4)).text()).isEqualTo("2");
    }

    @Test
    @DisplayName("분수를 더해 정수가 되면 정수로 보여 준다")
    void wholeResultsLookWhole() {
        assertThat(Num.fraction(1, 2).plus(Num.fraction(1, 2)).text()).isEqualTo("1");
        assertThat(Num.fraction(1, 2).plus(Num.fraction(1, 2)).isInteger()).isTrue();
    }

    @Test
    @DisplayName("0.5 와 1/2 는 같은 값이다 — 아이가 어느 쪽으로 써도 맞다")
    void sameValueDifferentLook() {
        assertThat(Num.decimal(5, 1).equalsValue(Num.fraction(1, 2))).isTrue();
        assertThat(Num.parse("0.5").equalsValue(Num.parse("1/2"))).isTrue();
        assertThat(Num.parse("0.75").equalsValue(Num.parse("3/4"))).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource({
            "12, 12", "'  12 ', 12", "-5, -5",
            "1.5, 1.5", "0.25, 0.25", "3.0, 3",
            "3/4, 3/4", "2/4, 1/2", "6/3, 2"
    })
    @DisplayName("아이가 적은 답을 읽는다")
    void parsing(String input, String expected) {
        assertThat(Num.parse(input)).isNotNull();
        assertThat(Num.parse(input).text()).isEqualTo(expected);
    }

    @Test
    @DisplayName("읽을 수 없는 것은 null — 틀렸다고 처리하면 된다")
    void rejectsGarbage() {
        assertThat(Num.parse(null)).isNull();
        assertThat(Num.parse("")).isNull();
        assertThat(Num.parse("abc")).isNull();
        assertThat(Num.parse("1/0")).isNull();
        assertThat(Num.parse("1//2")).isNull();
    }

    @Test
    @DisplayName("소수로 딱 떨어지지 않으면 분수로 보여 준다")
    void nonTerminatingStaysFraction() {
        assertThat(Num.fraction(1, 3).text()).isEqualTo("1/3");
        // 소수 모습으로 만들어졌어도 값이 1/3 이면 분수로 쓸 수밖에 없다
        assertThat(Num.decimal(1, 0).dividedBy(Num.of(3)).text()).isEqualTo("1/3");
    }

    @Test
    @DisplayName("소수와 분수가 섞이면 값을 잃지 않는 쪽을 따른다")
    void mixedForms() {
        assertThat(Num.decimal(5, 1).plus(Num.fraction(1, 4)).text()).isEqualTo("3/4");
        assertThat(Num.of(2).times(Num.decimal(15, 1)).text()).isEqualTo("3");
    }

    @Test
    @DisplayName("크기를 비교할 수 있다")
    void comparison() {
        assertThat(Num.fraction(1, 3)).isLessThan(Num.fraction(1, 2));
        assertThat(Num.decimal(25, 2)).isLessThan(Num.fraction(1, 2));
        assertThat(Num.of(3).compareTo(Num.fraction(6, 2))).isZero();
    }
}
