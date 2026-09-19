package com.dongsa.math.problem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("학습지 코드")
class ReadableCodeTest {

    @Test
    @DisplayName("시드를 코드로 바꿨다가 되돌리면 같은 값이다")
    void roundTrip() {
        Random rnd = new Random(42);
        for (int i = 0; i < 2000; i++) {
            long seed = Math.floorMod(rnd.nextLong(), ReadableCode.MAX_SEED);
            assertThat(ReadableCode.decode(ReadableCode.encode(seed))).isEqualTo(seed);
        }
    }

    @Test
    @DisplayName("헷갈리는 글자는 쓰지 않는다")
    void avoidsConfusingLetters() {
        assertThat(ReadableCode.ALPHABET).doesNotContain("I").doesNotContain("O")
                .doesNotContain("0").doesNotContain("1");
        for (long seed : new long[]{0, 1, 12345, ReadableCode.MAX_SEED - 1}) {
            assertThat(ReadableCode.encode(seed)).hasSize(6).matches("[" + ReadableCode.ALPHABET + "]{6}");
        }
    }

    @Test
    @DisplayName("소문자로 적어도 알아듣는다")
    void caseInsensitive() {
        String code = ReadableCode.encode(999);
        assertThat(ReadableCode.decode(code.toLowerCase())).isEqualTo(999L);
        assertThat(ReadableCode.decode("  " + code + " ")).isEqualTo(999L);
    }

    @Test
    @DisplayName("규칙에 안 맞는 코드는 null — 새로 뽑으라는 뜻이다")
    void rejectsGarbage() {
        assertThat(ReadableCode.decode(null)).isNull();
        assertThat(ReadableCode.decode("")).isNull();
        assertThat(ReadableCode.decode("ABC")).isNull();
        assertThat(ReadableCode.decode("ABCDEFG")).isNull();
        assertThat(ReadableCode.decode("ABCDE0")).isNull();   // 0 은 알파벳에 없다
    }
}
