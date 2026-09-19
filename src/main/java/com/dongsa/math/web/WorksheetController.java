package com.dongsa.math.web;

import com.dongsa.math.service.WorksheetService;
import com.dongsa.math.web.dto.WorksheetDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worksheets")
@Validated
public class WorksheetController {

    private final WorksheetService worksheets;

    public WorksheetController(WorksheetService worksheets) {
        this.worksheets = worksheets;
    }

    /** 학년을 고르면 낼 수 있는 유형과 숫자 범위를 알려 준다. */
    @GetMapping("/options")
    public GradeOptions options(@RequestParam @Min(1) @Max(6) int grade) {
        return worksheets.options(grade);
    }

    /**
     * 학습지 만들기. 선생님용이라 정답과 단서가 함께 나온다.
     * 응답의 code 를 적어 두었다가 다시 넣으면 같은 문제가 그대로 나온다.
     */
    @PostMapping
    public WorksheetResponse generate(@Valid @RequestBody GenerateRequest req) {
        return worksheets.generate(req);
    }
}
