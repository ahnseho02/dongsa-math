package com.dongsa.math.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학원. 이 서비스의 모든 데이터는 학원 하나에 묶인다.
 * code 는 학생이 로그인할 때 입력하는 6자리 코드다.
 */
@Entity
@Getter
@Table(name = "academy", uniqueConstraints = @UniqueConstraint(name = "uk_academy_code", columnNames = "code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Academy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 6)
    private String code;

    public Academy(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public void rename(String name) {
        this.name = name;
    }
}
