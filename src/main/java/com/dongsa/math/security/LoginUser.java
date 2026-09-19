package com.dongsa.math.security;

import com.dongsa.math.domain.TeacherRole;

/**
 * 로그인한 사람. 컨트롤러는 항상 이 값을 통해서만 자기 학원 데이터에 접근한다.
 * academyId 가 여기 들어 있기 때문에 다른 학원 데이터는 애초에 조회되지 않는다.
 */
public record LoginUser(Kind kind, Long id, Long academyId, String name, TeacherRole role) {

    public enum Kind { TEACHER, STUDENT }

    public boolean isTeacher() { return kind == Kind.TEACHER; }

    public boolean isOwner() { return kind == Kind.TEACHER && role == TeacherRole.OWNER; }

    public String authority() { return isTeacher() ? "ROLE_TEACHER" : "ROLE_STUDENT"; }
}
