package com.dongsa.math.security;

import com.dongsa.math.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper mapper;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, ObjectMapper mapper,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.mapper = mapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(corsSource()))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))   // H2 콘솔
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/student/login").permitAll()
                .requestMatchers("/h2-console/**", "/actuator/health", "/actuator/health/**").permitAll()
                // 프런트 자체는 누구나 받아 간다. 그 안에서 부르는 /api/** 는 여전히 토큰이 필요하다.
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.js", "/style.css",
                                 "/icon.svg", "/manifest.webmanifest", "/favicon.ico").permitAll()
                .requestMatchers("/api/my/**").hasRole("STUDENT")
                .requestMatchers("/api/academy/**", "/api/classrooms/**", "/api/students/**",
                                 "/api/worksheets/**", "/api/assignments/**",
                                 "/api/games/**").hasRole("TEACHER")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(res, 401, ApiError.of("UNAUTHORIZED", "로그인이 필요합니다.")))
                .accessDeniedHandler((req, res, ex) -> write(res, 403, ApiError.of("FORBIDDEN", "권한이 없습니다."))))
            .addFilterBefore(jwtFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void write(jakarta.servlet.http.HttpServletResponse res, int status, ApiError body) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), body);
    }

    /**
     * 프런트(모바일 웹)는 별도 주소에서 뜬다.
     * 개발 기본값은 전부 열려 있고, 운영에서는 CORS_ORIGINS 환경변수로 실제 도메인만 넣는다.
     */
    private CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(allowedOrigins);
        c.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
