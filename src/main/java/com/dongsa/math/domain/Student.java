package com.dongsa.math.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학생. 초등학생이라 이메일과 비밀번호를 쓰지 않는다.
 * 학원 코드 + 이름 + 네 자리 PIN 으로 로그인한다. PIN 도 해시로만 저장한다.
 */
@Entity
@Getter
@Table(name = "student", indexes = @Index(name = "ix_student_academy_name", columnList = "academy_id,name"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "pin_hash", nullable = false, length = 100)
    private String pinHash;

    /** 초등 1~6학년. 문제 난이도를 정하는 기준이 된다. */
    @Column(nullable = false)
    private int grade;

    @Column(nullable = false)
    private boolean active = true;

    public Student(Academy academy, Classroom classroom, String name, String pinHash, int grade) {
        this.academy = academy;
        this.classroom = classroom;
        this.name = name;
        this.pinHash = pinHash;
        this.grade = grade;
        this.active = true;
    }

    public void update(String name, Integer grade, Classroom classroom) {
        if (name != null) this.name = name;
        if (grade != null) this.grade = grade;
        this.classroom = classroom;
    }

    public void changePin(String pinHash) {
        this.pinHash = pinHash;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
