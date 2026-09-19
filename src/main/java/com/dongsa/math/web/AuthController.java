package com.dongsa.math.web;

import com.dongsa.math.security.LoginUser;
import com.dongsa.math.service.AuthService;
import com.dongsa.math.web.dto.AuthDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** 원장 회원가입 — 계정과 학원이 한 번에 만들어진다. */
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.signup(req));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return auth.login(req, clientIp(http));
    }

    /** 학생 로그인 — 학원 코드 + 이름 + PIN 네 자리. */
    @PostMapping("/student/login")
    public TokenResponse studentLogin(@Valid @RequestBody StudentLoginRequest req, HttpServletRequest http) {
        return auth.studentLogin(req, clientIp(http));
    }

    /**
     * 프록시 뒤에 있으면 getRemoteAddr 은 프록시 주소를 준다.
     * server.forward-headers-strategy=framework 이 X-Forwarded-For 를 반영해 주므로 그대로 쓴다.
     */
    private String clientIp(HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal LoginUser me) {
        return auth.me(me);
    }
}
