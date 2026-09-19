package com.dongsa.math.problem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("한국어 조사 처리")
class JosaTest {

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @CsvSource({
            // 받침 있는 이름
            "민우, '{은/는}', 민우는",      // ㅜ 받침 없음
            "지혜, '{은/는}', 지혜는",
            "하린, '{은/는}', 하린은",      // ㄴ 받침
            "건우, '{이/가}', 건우가",
            "서윤, '{이/가}', 서윤이",      // ㄴ 받침
            "준호, '{이/가}', 준호가",
            "연필, '{을/를}', 연필을",      // ㄹ 받침
            "사과, '{을/를}', 사과를",
            "책, '{과/와}', 책과",
            "의자, '{과/와}', 의자와"
    })
    void picksByFinalConsonant(String word, String form, String expected) {
        assertThat(Josa.resolve(word + form)).isEqualTo(expected);
    }

    @Test
    @DisplayName("숫자로 끝나도 읽는 소리로 판단한다")
    void numbersUseTheirSpokenForm() {
        assertThat(Josa.resolve("3{은/는}")).isEqualTo("3은");    // 삼 — ㅁ 받침
        assertThat(Josa.resolve("2{은/는}")).isEqualTo("2는");    // 이 — 받침 없음
        assertThat(Josa.resolve("100{이/가}")).isEqualTo("100이"); // 백 — ㄱ 받침
        assertThat(Josa.resolve("20{이/가}")).isEqualTo("20이");   // 이십 — ㅂ 받침
    }

    @Test
    @DisplayName("'으로'는 ㄹ 받침에서 '로'가 된다")
    void euroIsSpecial() {
        assertThat(Josa.resolve("연필{으로/로}")).isEqualTo("연필로");   // ㄹ
        assertThat(Josa.resolve("종이{으로/로}")).isEqualTo("종이로");   // 받침 없음
        assertThat(Josa.resolve("손{으로/로}")).isEqualTo("손으로");     // ㄴ
    }

    @Test
    @DisplayName("한 문장에 여러 개가 있어도 각각 앞 글자를 본다")
    void manyInOneSentence() {
        String result = Josa.resolve("{name}{은/는} 사과{을/를} 샀고 서윤{이/가} 연필{을/를} 샀다"
                .replace("{name}", "하린"));
        assertThat(result).isEqualTo("하린은 사과를 샀고 서윤이 연필을 샀다");
    }

    @Test
    @DisplayName("누를 수 있는 단어 표시(«»)는 조사 판단에서 건너뛴다")
    void markersAreIgnored() {
        assertThat(Josa.resolve("«사과»{을/를}")).isEqualTo("«사과»를");
        assertThat(Josa.resolve("«연필»{을/를}")).isEqualTo("«연필»을");
    }
}
