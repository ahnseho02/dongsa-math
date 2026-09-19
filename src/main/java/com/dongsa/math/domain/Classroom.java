package com.dongsa.math.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 반. 학년이 섞인 반도 있으므로 grade 는 비워 둘 수 있다. */
@Entity
@Getter
@Table(name = "classroom")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classroom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Column(nullable = false, length = 40)
    private String name;

    private Integer grade;

    public Classroom(Academy academy, String name, Integer grade) {
        this.academy = academy;
        this.name = name;
        this.grade = grade;
    }

    public void update(String name, Integer grade) {
        this.name = name;
        this.grade = grade;
    }
}
