package com.dongsa.math.security;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 시도 제한.
 *
 * 학생 PIN 은 네 자리라 1만 가지뿐이다. 학원 코드와 이름만 알면 초당 수십 번 넣어 보는 것으로 뚫린다.
 * PIN 을 길게 만들 수는 없으므로(초등학생이 쓴다) 시도 횟수 쪽을 막는다.
 *
 * 두 가지를 같이 센다.
 *   - 계정별: 한 아이의 PIN 을 계속 찍어 보는 것
 *   - IP별:   이름을 바꿔 가며 훑는 것
 *
 * 한 대짜리 서버를 전제로 메모리에 들고 있다. 서버를 여러 대로 늘리면 Redis 로 옮겨야 한다.
 */
@Component
public class LoginAttemptLimiter {

    /** 항목이 이만큼 쌓이면 지난 것들을 치운다. */
    private static final int PRUNE_THRESHOLD = 10_000;

    private final int maxFailures;
    private final int maxFailuresPerIp;
    private final Duration window;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(@Value("${app.login.max-failures}") int maxFailures,
                               @Value("${app.login.max-failures-per-ip}") int maxFailuresPerIp,
                               @Value("${app.login.window}") Duration window) {
        this.maxFailures = maxFailures;
        this.maxFailuresPerIp = maxFailuresPerIp;
        this.window = window;
    }

    /** 로그인을 시도하기 전에 부른다. 이미 막혔으면 비밀번호를 대조하지도 않는다. */
    public void checkAllowed(String accountKey, String ip) {
        Instant now = Instant.now();
        if (exceeded("a:" + accountKey, maxFailures, now) || exceeded("i:" + ip, maxFailuresPerIp, now)) {
            throw new ApiException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }

    public void recordFailure(String accountKey, String ip) {
        Instant now = Instant.now();
        bump("a:" + accountKey, now);
        bump("i:" + ip, now);
        pruneIfCrowded(now);
    }

    /** 제대로 들어왔으면 그 계정의 실패 기록은 지운다. IP 기록은 남긴다. */
    public void recordSuccess(String accountKey) {
        counters.remove("a:" + accountKey);
    }

    /** 학생: 같은 학원의 같은 이름을 한 덩어리로 센다. */
    public static String studentKey(String academyCode, String name) {
        return "s|" + academyCode.strip().toUpperCase() + "|" + name.strip();
    }

    public static String teacherKey(String email) {
        return "t|" + email.strip().toLowerCase();
    }

    // ── 내부 ──

    private boolean exceeded(String key, int limit, Instant now) {
        Counter counter = counters.get(key);
        return counter != null && counter.isLive(now) && counter.count >= limit;
    }

    private void bump(String key, Instant now) {
        counters.compute(key, (k, existing) -> {
            if (existing == null || !existing.isLive(now)) {
                return new Counter(1, now.plus(window));
            }
            return new Counter(existing.count + 1, existing.resetAt);
        });
    }

    private void pruneIfCrowded(Instant now) {
        if (counters.size() > PRUNE_THRESHOLD) {
            counters.values().removeIf(c -> !c.isLive(now));
        }
    }

    /** 창이 지나면 다시 0 부터 센다. */
    private record Counter(int count, Instant resetAt) {

        boolean isLive(Instant now) {
            return now.isBefore(resetAt);
        }
    }
}
