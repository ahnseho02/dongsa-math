package com.dongsa.math.web;

import com.dongsa.math.security.LoginUser;
import com.dongsa.math.service.StudentReportService;
import com.dongsa.math.service.StudentService;
import com.dongsa.math.web.dto.ReportDtos.AcademyStudentRow;
import com.dongsa.math.web.dto.ReportDtos.StudentReport;
import com.dongsa.math.web.dto.StudentDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentReportService reports;

    public StudentController(StudentService studentService, StudentReportService reports) {
        this.studentService = studentService;
        this.reports = reports;
    }

    @GetMapping
    public List<StudentResponse> list(@AuthenticationPrincipal LoginUser me,
                                      @RequestParam(required = false) Long classroomId) {
        return studentService.list(me, classroomId);
    }

    /** 학생 등록. 응답에 PIN 평문이 딱 한 번 들어온다 — 아이에게 알려 주고 끝이다. */
    @PostMapping
    public ResponseEntity<StudentWithPinResponse> create(@AuthenticationPrincipal LoginUser me,
                                                         @Valid @RequestBody CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(me, req));
    }

    @PatchMapping("/{id}")
    public StudentResponse update(@AuthenticationPrincipal LoginUser me,
                                  @PathVariable Long id,
                                  @Valid @RequestBody UpdateRequest req) {
        return studentService.update(me, id, req);
    }

    /** PIN 을 잊었을 때 새로 발급. 비워서 보내면 자동으로 만들어 준다. */
    @PatchMapping("/{id}/pin")
    public StudentWithPinResponse resetPin(@AuthenticationPrincipal LoginUser me,
                                           @PathVariable Long id,
                                           @RequestBody(required = false) @Valid PinRequest req) {
        return studentService.resetPin(me, id, req);
    }

    /** 학원 학생 전체 성적을 한 줄씩. 약한 아이가 위로 온다. */
    @GetMapping("/report")
    public List<AcademyStudentRow> overview(@AuthenticationPrincipal LoginUser me) {
        return reports.academyOverview(me);
    }

    /** 한 아이가 그동안 푼 것 전부 — 단계별·유형별·과제별. */
    @GetMapping("/{id}/report")
    public StudentReport report(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        return reports.forStudent(me, id);
    }

    /**
     * 학생을 완전히 삭제한다. 성적까지 같이 사라지고 되돌릴 수 없다.
     * 평소에는 아래 /active 로 중지만 하는 편이 낫다.
     */
    @DeleteMapping("/{id}")
    public DeletedResponse delete(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        var removed = studentService.delete(me, id);
        return new DeletedResponse(removed.id(), removed.name(), removed.assignments(), removed.submissions());
    }

    @PatchMapping("/{id}/active")
    public StudentResponse setActive(@AuthenticationPrincipal LoginUser me,
                                     @PathVariable Long id,
                                     @RequestBody ActiveRequest req) {
        return studentService.setActive(me, id, req.active());
    }
}
