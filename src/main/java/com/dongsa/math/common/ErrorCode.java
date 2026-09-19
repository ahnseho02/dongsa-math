package com.dongsa.math.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    EMAIL_TAKEN(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 맞지 않습니다."),
    BAD_STUDENT_LOGIN(HttpStatus.UNAUTHORIZED, "학원 코드, 이름 또는 PIN이 맞지 않습니다."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "사용이 중지된 계정입니다. 선생님께 문의하세요."),
    OWNER_ONLY(HttpStatus.FORBIDDEN, "원장 선생님만 할 수 있습니다."),
    ACADEMY_NOT_FOUND(HttpStatus.NOT_FOUND, "학원을 찾을 수 없습니다."),
    TEACHER_NOT_FOUND(HttpStatus.NOT_FOUND, "선생님을 찾을 수 없습니다."),
    CLASSROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "반을 찾을 수 없습니다."),
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."),
    CLASSROOM_NAME_TAKEN(HttpStatus.CONFLICT, "같은 이름의 반이 이미 있습니다."),
    DUPLICATE_STUDENT_PIN(HttpStatus.CONFLICT, "같은 이름의 학생이 같은 PIN을 쓰고 있습니다. 다른 PIN을 정해 주세요."),
    CANNOT_DISABLE_SELF(HttpStatus.BAD_REQUEST, "자기 계정은 중지할 수 없습니다."),
    NO_TEMPLATE_FOR_GRADE(HttpStatus.BAD_REQUEST, "그 학년에 낼 수 있는 문제 유형이 없습니다."),
    ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다."),
    NO_TARGET_STUDENT(HttpStatus.BAD_REQUEST, "과제를 받을 학생이 없습니다. 반이나 학생을 골라 주세요."),
    ASSIGNMENT_CLOSED(HttpStatus.CONFLICT, "마감된 과제입니다."),
    PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "그 번호의 문제가 없습니다."),
    STEP_OUT_OF_ORDER(HttpStatus.CONFLICT, "앞 단계를 먼저 풀어야 합니다."),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후에 다시 해 주세요."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "그 게임을 찾을 수 없습니다."),
    GAME_ALREADY_FINISHED(HttpStatus.CONFLICT, "이미 끝난 게임입니다."),
    GAME_TIME_OVER(HttpStatus.CONFLICT, "시간이 지나 기록이 남지 않았습니다. 다시 해 보세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}
