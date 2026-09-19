package com.dongsa.math.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("배포 플랫폼의 DB 주소 변환")
class DatabaseUrlNormalizerTest {

    @Test
    @DisplayName("Railway 형식을 JDBC 형식으로 바꾼다")
    void convertsPlatformUrl() {
        var parsed = DatabaseUrlNormalizer.parse(
                "postgresql://postgres:s3cr3t@containers-us-west-1.railway.app:6543/railway");

        assertThat(parsed).isNotNull();
        assertThat(parsed.jdbcUrl())
                .isEqualTo("jdbc:postgresql://containers-us-west-1.railway.app:6543/railway");
        assertThat(parsed.user()).isEqualTo("postgres");
        assertThat(parsed.password()).isEqualTo("s3cr3t");
    }

    @Test
    @DisplayName("postgres:// 도 받고, 포트가 없으면 5432 로 본다")
    void handlesShortSchemeAndDefaultPort() {
        var parsed = DatabaseUrlNormalizer.parse("postgres://u:p@db.internal/dongsa");
        assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://db.internal:5432/dongsa");
    }

    @Test
    @DisplayName("sslmode 같은 옵션은 그대로 옮긴다")
    void keepsQueryString() {
        var parsed = DatabaseUrlNormalizer.parse("postgresql://u:p@host:5432/db?sslmode=require");
        assertThat(parsed.jdbcUrl()).endsWith("/db?sslmode=require");
    }

    @Test
    @DisplayName("이미 JDBC 형식이면 손대지 않는다")
    void leavesJdbcAlone() {
        assertThat(DatabaseUrlNormalizer.parse("jdbc:postgresql://localhost:5432/dongsa")).isNull();
        assertThat(DatabaseUrlNormalizer.parse(null)).isNull();
        assertThat(DatabaseUrlNormalizer.parse("")).isNull();
        assertThat(DatabaseUrlNormalizer.parse("mysql://u:p@host/db")).isNull();
    }
}
