package com.dongsa.math.web;

import com.dongsa.math.security.LoginUser;
import com.dongsa.math.service.AcademyService;
import com.dongsa.math.web.dto.AcademyDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {

    private final AcademyService academy;

    public AcademyController(AcademyService academy) {
        this.academy = academy;
    }

    /** 학원 정보와 학생이 로그인할 때 쓰는 코드. */
    @GetMapping
    public AcademyResponse get(@AuthenticationPrincipal LoginUser me) {
        return academy.get(me);
    }

    @PatchMapping
    public AcademyResponse rename(@AuthenticationPrincipal LoginUser me, @Valid @RequestBody RenameRequest req) {
        return academy.rename(me, req);
    }

    @GetMapping("/teachers")
    public List<TeacherResponse> teachers(@AuthenticationPrincipal LoginUser me) {
        return academy.listTeachers(me);
    }

    /** 원장만. 강사 계정을 만들어 주고 첫 비밀번호를 직접 전달한다. */
    @PostMapping("/teachers")
    public ResponseEntity<TeacherResponse> createTeacher(@AuthenticationPrincipal LoginUser me,
                                                         @Valid @RequestBody CreateTeacherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academy.createTeacher(me, req));
    }

    /** 원장만. 그만둔 강사의 로그인을 막는다. 기록은 남긴다. */
    @PatchMapping("/teachers/{id}/active")
    public TeacherResponse setTeacherActive(@AuthenticationPrincipal LoginUser me,
                                            @PathVariable Long id,
                                            @RequestBody ActiveRequest req) {
        return academy.setTeacherActive(me, id, req.active());
    }
}
