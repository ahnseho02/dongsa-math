package com.dongsa.math.service;

import com.dongsa.math.problem.ReadableCode;
import com.dongsa.math.repository.AcademyRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 학원 코드 만들기.
 * 아이가 손으로 입력하는 값이라 헷갈리는 글자(I, O, 0, 1)를 뺀 32자만 쓴다.
 */
@Component
public class AcademyCodeGenerator {

    private static final String ALPHABET = ReadableCode.ALPHABET;
    private static final int LENGTH = ReadableCode.LENGTH;
    private static final int MAX_TRIES = 50;

    private final AcademyRepository academies;
    private final SecureRandom random = new SecureRandom();

    public AcademyCodeGenerator(AcademyRepository academies) {
        this.academies = academies;
    }

    public String generate() {
        for (int i = 0; i < MAX_TRIES; i++) {
            String code = randomCode();
            if (!academies.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("학원 코드를 만들지 못했습니다. 코드 길이를 늘려야 합니다.");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
