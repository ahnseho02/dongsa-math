package com.dongsa.math.web;

import com.dongsa.math.security.LoginUser;
import com.dongsa.math.service.AssignmentService;
import com.dongsa.math.web.dto.AssignmentDtos.*;
import com.dongsa.math.web.dto.ReportDtos.AssignmentReport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 선생님이 과제를 내고 결과를 보는 쪽. */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public ResponseEntity<AssignmentDetail> create(@AuthenticationPrincipal LoginUser me,
                                                   @Valid @RequestBody CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.create(me, req));
    }

    @GetMapping
    public List<AssignmentSummary> list(@AuthenticationPrincipal LoginUser me) {
        return assignmentService.list(me);
    }

    /** 누가 어디까지 풀었는지. */
    @GetMapping("/{id}")
    public AssignmentDetail detail(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        return assignmentService.detail(me, id);
    }

    /** 단계별·유형별 취약점. 같은 점수라도 처방이 다른 이유가 여기 나온다. */
    @GetMapping("/{id}/report")
    public AssignmentReport report(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        return assignmentService.report(me, id);
    }

    @PatchMapping("/{id}/closed")
    public AssignmentSummary setClosed(@AuthenticationPrincipal LoginUser me,
                                       @PathVariable Long id,
                                       @RequestBody CloseRequest req) {
        return assignmentService.setClosed(me, id, req.closed());
    }
}
