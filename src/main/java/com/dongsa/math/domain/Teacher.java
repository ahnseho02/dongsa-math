package com.dongsa.math.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 원장·강사 계정. 이메일과 비밀번호로 로그인한다. */
@Entity
@Getter
@Table(name = "teacher", uniqueConstraints = @UniqueConstraint(name = "uk_teacher_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Teacher extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TeacherRole role;

    @Column(nullable = false)
    private boolean active = true;

    public Teacher(Academy academy, String email, String passwordHash, String name, TeacherRole role) {
        this.academy = academy;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.active = true;
    }

    public boolean isOwner() {
        return role == TeacherRole.OWNER;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
