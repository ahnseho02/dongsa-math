package com.dongsa.math.web;

import com.dongsa.math.security.LoginUser;
import com.dongsa.math.service.ClassroomService;
import com.dongsa.math.web.dto.ClassroomDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomService classrooms;

    public ClassroomController(ClassroomService classrooms) {
        this.classrooms = classrooms;
    }

    @GetMapping
    public List<ClassroomResponse> list(@AuthenticationPrincipal LoginUser me) {
        return classrooms.list(me);
    }

    @PostMapping
    public ResponseEntity<ClassroomResponse> create(@AuthenticationPrincipal LoginUser me,
                                                    @Valid @RequestBody SaveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classrooms.create(me, req));
    }

    @PatchMapping("/{id}")
    public ClassroomResponse update(@AuthenticationPrincipal LoginUser me,
                                    @PathVariable Long id,
                                    @Valid @RequestBody SaveRequest req) {
        return classrooms.update(me, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        classrooms.delete(me, id);
        return ResponseEntity.noContent().build();
    }
}
