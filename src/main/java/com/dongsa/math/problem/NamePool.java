package com.dongsa.math.problem;

import java.util.List;
import java.util.Random;

/** 문제 속에 등장하는 이름. 실제 학생 이름은 쓰지 않는다. */
public final class NamePool {

    private NamePool() {}

    private static final List<String> NAMES = List.of(
            "민우", "지혜", "서윤", "준호", "은수", "하린",
            "도윤", "채원", "시우", "예은", "태린", "건우");

    public static String pick(Random rnd) {
        return NAMES.get(rnd.nextInt(NAMES.size()));
    }

    /** 두 사람을 비교하는 문장에서는 이름이 겹치면 안 된다. */
    public static String pickOther(String taken, Random rnd) {
        String name;
        do {
            name = pick(rnd);
        } while (name.equals(taken));
        return name;
    }
}
