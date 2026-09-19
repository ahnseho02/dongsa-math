package com.dongsa.math.service;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import com.dongsa.math.domain.Academy;
import com.dongsa.math.domain.Classroom;
import com.dongsa.math.repository.AcademyRepository;
import com.dongsa.math.repository.AssignmentRepository;
import com.dongsa.math.repository.ClassroomRepository;
import com.dongsa.math.repository.StudentRepository;
import com.dongsa.math.security.LoginUser;
import com.dongsa.math.web.dto.ClassroomDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClassroomService {

    private final ClassroomRepository classrooms;
    private final AcademyRepository academies;
    private final StudentRepository students;
    private final AssignmentRepository assignments;

    public ClassroomService(ClassroomRepository classrooms, AcademyRepository academies,
                            StudentRepository students, AssignmentRepository assignments) {
        this.classrooms = classrooms;
        this.academies = academies;
        this.students = students;
        this.assignments = assignments;
    }

    public List<ClassroomResponse> list(LoginUser me) {
        return classrooms.findByAcademyIdOrderByIdAsc(me.academyId()).stream().map(this::toDto).toList();
    }

    @Transactional
    public ClassroomResponse create(LoginUser me, SaveRequest req) {
        String name = req.name().trim();
        if (classrooms.existsByAcademyIdAndName(me.academyId(), name)) {
            throw new ApiException(ErrorCode.CLASSROOM_NAME_TAKEN);
        }
        Academy academy = academies.findById(me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.ACADEMY_NOT_FOUND));
        return toDto(classrooms.save(new Classroom(academy, name, req.grade())));
    }

    @Transactional
    public ClassroomResponse update(LoginUser me, Long id, SaveRequest req) {
        Classroom classroom = mine(me, id);
        String name = req.name().trim();
        if (!classroom.getName().equals(name) && classrooms.existsByAcademyIdAndName(me.academyId(), name)) {
            throw new ApiException(ErrorCode.CLASSROOM_NAME_TAKEN);
        }
        classroom.update(name, req.grade());
        return toDto(classroom);
    }

    /** 반을 지워도 학생과 과제 기록은 남는다. 반 연결만 끊는다. */
    @Transactional
    public void delete(LoginUser me, Long id) {
        Classroom classroom = mine(me, id);
        students.findByAcademyIdAndClassroomIdOrderByNameAsc(me.academyId(), id)
                .forEach(s -> s.update(null, null, null));
        assignments.detachFromClassroom(id);
        classrooms.delete(classroom);
    }

    Classroom mine(LoginUser me, Long id) {
        return classrooms.findByIdAndAcademyId(id, me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.CLASSROOM_NOT_FOUND));
    }

    private ClassroomResponse toDto(Classroom c) {
        long count = students.findByAcademyIdAndClassroomIdOrderByNameAsc(c.getAcademy().getId(), c.getId()).size();
        return new ClassroomResponse(c.getId(), c.getName(), c.getGrade(), count);
    }
}
