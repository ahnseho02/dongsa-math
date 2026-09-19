package com.dongsa.math.security;

import com.dongsa.math.domain.TeacherRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration teacherTtl;
    private final Duration studentTtl;

    /** 개발용 기본값을 알아보기 위한 표식. 이 값으로 운영에 뜨면 토큰을 누구나 위조할 수 있다. */
    static final String DEV_SECRET_MARK = "dev-only-";
    private static final int MIN_SECRET_BYTES = 32;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.teacher-ttl}") Duration teacherTtl,
                       @Value("${app.jwt.student-ttl}") Duration studentTtl,
                       Environment environment) {
        verify(secret, Arrays.asList(environment.getActiveProfiles()).contains("prod"));
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.teacherTtl = teacherTtl;
        this.studentTtl = studentTtl;
    }

    /**
     * 비밀키 점검. 문제가 있으면 서버가 아예 뜨지 않는다.
     * 잘못된 키로 조용히 떠 있는 것보다 시작을 못 하는 편이 낫다.
     */
    static void verify(String secret, boolean production) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret 은 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다 (HMAC-SHA256 요구사항).");
        }
        if (production && secret.startsWith(DEV_SECRET_MARK)) {
            throw new IllegalStateException(
                    "운영 프로필인데 개발용 기본 비밀키가 그대로입니다. JWT_SECRET 환경변수를 설정하세요.");
        }
    }

    public String issue(LoginUser user) {
        Duration ttl = user.isTeacher() ? teacherTtl : studentTtl;
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("kind", user.kind().name())
                .claim("aid", user.academyId())
                .claim("name", user.name())
                .claim("role", user.role() == null ? null : user.role().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }

    /** 토큰이 깨졌거나 만료됐으면 null. 호출한 쪽에서 익명으로 처리한다. */
    public LoginUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String roleName = c.get("role", String.class);
            return new LoginUser(
                    LoginUser.Kind.valueOf(c.get("kind", String.class)),
                    Long.valueOf(c.getSubject()),
                    c.get("aid", Number.class).longValue(),
                    c.get("name", String.class),
                    roleName == null ? null : TeacherRole.valueOf(roleName));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public long studentTtlSeconds() { return studentTtl.toSeconds(); }
    public long teacherTtlSeconds() { return teacherTtl.toSeconds(); }
}
