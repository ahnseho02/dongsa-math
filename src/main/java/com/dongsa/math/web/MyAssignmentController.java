package com.dongsa.math.web;

import com.dongsa.math.security.LoginUser;
import com.dongsa.math.service.SolveService;
import com.dongsa.math.web.dto.SolveDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 아이가 문제를 푸는 쪽. 응답에 정답과 단서는 들어 있지 않다. */
@RestController
@RequestMapping("/api/my/assignments")
public class MyAssignmentController {

    private final SolveService solve;

    public MyAssignmentController(SolveService solve) {
        this.solve = solve;
    }

    @GetMapping
    public List<MyAssignment> list(@AuthenticationPrincipal LoginUser me) {
        return solve.myAssignments(me);
    }

    @GetMapping("/{id}")
    public MyAssignmentDetail open(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        return solve.open(me, id);
    }

    /** 한 단계 제출. 같은 단계를 또 보내면 처음 결과가 그대로 온다. */
    @PostMapping("/{id}/answers")
    public AnswerResult answer(@AuthenticationPrincipal LoginUser me,
                               @PathVariable Long id,
                               @Valid @RequestBody AnswerRequest req) {
        return solve.submit(me, id, req);
    }
}
