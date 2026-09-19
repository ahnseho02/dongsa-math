package com.dongsa.math.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JWT 비밀키 점검")
class JwtSecretTest {

    private static final String REAL = "a-long-enough-production-secret-value-32+";

    @Test
    @DisplayName("32바이트보다 짧으면 서버가 뜨지 않는다")
    void tooShortIsRejected() {
        assertThatThrownBy(() -> JwtProvider.verify("short", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32바이트");
    }

    @Test
    @DisplayName("운영에서 개발용 기본키를 쓰면 서버가 뜨지 않는다")
    void devSecretIsRejectedInProduction() {
        String dev = JwtProvider.DEV_SECRET_MARK + "secret-please-change-this-to-a-long-value";

        assertThatCode(() -> JwtProvider.verify(dev, false))
                .as("개발에서는 그대로 써도 된다").doesNotThrowAnyException();

        assertThatThrownBy(() -> JwtProvider.verify(dev, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("제대로 된 키는 통과한다")
    void properSecretPasses() {
        assertThatCode(() -> JwtProvider.verify(REAL, true)).doesNotThrowAnyException();
    }
}
