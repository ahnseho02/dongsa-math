package com.dongsa.math.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * 배포 플랫폼이 주는 DB 주소를 JDBC 형식으로 바꿔 준다.
 *
 * Railway·Heroku·Render 는 DATABASE_URL 을 이렇게 준다.
 *     postgresql://사용자:비밀번호@호스트:5432/디비
 * 그런데 JDBC 가 필요한 형식은 이렇다.
 *     jdbc:postgresql://호스트:5432/디비   (+ 사용자와 비밀번호는 따로)
 *
 * 그대로 넣으면 "Driver claims to not accept jdbcUrl" 같은 알아보기 어려운 오류가 난다.
 * 사람이 형식을 외우게 하는 대신 여기서 바꾼다. 이미 jdbc: 로 시작하면 손대지 않는다.
 *
 * 스프링 컨텍스트가 만들어지기 전에 돌아야 해서 EnvironmentPostProcessor 로 등록한다.
 */
public class DatabaseUrlNormalizer implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "databaseUrlNormalizer";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = environment.getProperty("DATABASE_URL");
        Parsed parsed = parse(raw);
        if (parsed == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("DATABASE_URL", parsed.jdbcUrl());
        // 사용자와 비밀번호를 따로 넣어 줬다면 그쪽을 존중한다
        if (parsed.user() != null && environment.getProperty("DATABASE_USER") == null) {
            values.put("DATABASE_USER", parsed.user());
        }
        if (parsed.password() != null && environment.getProperty("DATABASE_PASSWORD") == null) {
            values.put("DATABASE_PASSWORD", parsed.password());
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, values));
    }

    /** 바꿀 것이 없으면 null. 순수 함수라 테스트하기 쉽다. */
    static Parsed parse(String raw) {
        if (raw == null || raw.isBlank() || raw.startsWith("jdbc:")) {
            return null;
        }
        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(raw);
        } catch (URISyntaxException e) {
            return null;
        }
        String host = uri.getHost();
        if (host == null) {
            return null;
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();

        String user = null;
        String password = null;
        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int colon = userInfo.indexOf(':');
            user = colon < 0 ? userInfo : userInfo.substring(0, colon);
            password = colon < 0 ? null : userInfo.substring(colon + 1);
        }
        return new Parsed("jdbc:postgresql://" + host + ":" + port + "/" + database + query, user, password);
    }

    record Parsed(String jdbcUrl, String user, String password) {}
}
