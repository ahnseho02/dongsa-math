package com.dongsa.math.problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 조건을 지키는 숫자 뽑기.
 *
 * 그냥 난수를 쓰면 "받아올림 있는 세 자리 덧셈"을 연습시킬 수 없다.
 * 그래서 조건에 맞을 때까지 다시 뽑는다. 조건을 만족하는 조합이 충분히 많아서
 * 보통 몇 번 안에 걸리고, 그래도 못 찾으면 마지막에 뽑은 값을 쓴다
 * (예: 한 자리 수에서 '받아올림 없는 뺄셈'처럼 경우의 수가 적은 조합).
 */
public final class NumberFactory {

    private NumberFactory() {}

    private static final int MAX_TRIES = 400;

    public static List<Integer> generate(NumberPattern pattern, GradeRange grade, boolean carry, Random rnd) {
        return switch (pattern) {
            case ADD_PAIR -> addPair(grade, carry, rnd);
            case ADD_SMALL_SECOND -> addSmallSecond(grade, carry, rnd);
            case SUB_PAIR -> subPair(grade, carry, rnd);
            case ADD_TRIPLE -> addTriple(grade, carry, rnd);
            case MUL_PAIR -> mulPair(grade, rnd);
            case DIV_PAIR -> divPair(grade, rnd);
        };
    }

    private static List<Integer> addPair(GradeRange g, boolean carry, Random rnd) {
        if (!carry) {
            return buildWithoutCarry(2, g, rnd);
        }
        List<Integer> last = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            last = List.of(between(g.min(), g.max(), rnd), between(g.min(), g.max(), rnd));
            if (hasCarry(last)) {
                return last;
            }
        }
        return last;
    }

    private static List<Integer> addSmallSecond(GradeRange g, boolean carry, Random rnd) {
        int smallMax = Math.max(2, g.max() / 5);
        int smallMin = Math.max(2, smallMax / 5);
        List<Integer> last = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            last = List.of(between(g.min(), g.max(), rnd), between(smallMin, smallMax, rnd));
            if (hasCarry(last) == carry) {
                return last;
            }
        }
        return last;
    }

    private static List<Integer> subPair(GradeRange g, boolean borrow, Random rnd) {
        // 한 자리 수끼리는 받아내림이 생길 수 없다 (앞의 수가 항상 더 크므로).
        // 초1에서 '받아내림 있음'을 골라도 조용히 없는 문제를 낸다 — 수학적으로 불가능하기 때문이다.
        boolean wanted = borrow && borrowPossible(g);
        List<Integer> last = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            int a = between(g.min() + 1, g.max(), rnd);
            int b = between(g.min(), a - 1, rnd);
            last = List.of(a, b);
            if (hasBorrow(a, b) == wanted) {
                return last;
            }
        }
        return last;
    }

    private static List<Integer> addTriple(GradeRange g, boolean carry, Random rnd) {
        if (!carry) {
            return buildWithoutCarry(3, g, rnd);
        }
        List<Integer> last = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            last = List.of(between(g.min(), g.max(), rnd),
                           between(g.min(), g.max(), rnd),
                           between(g.min(), g.max(), rnd));
            if (hasCarry(last)) {
                return last;
            }
        }
        return last;
    }

    /** 곱셈은 자릿수 대신 구구단 범위로 조절한다. 다섯 자리 × 아홉은 초등 과정이 아니다. */
    private static List<Integer> mulPair(GradeRange g, Random rnd) {
        int groupMax = g.grade() <= 2 ? 9 : (g.grade() <= 4 ? 19 : 99);
        return List.of(between(2, groupMax, rnd), between(2, 9, rnd));
    }

    /** 몫과 나누는 수를 먼저 정하고 곱해서 전체를 만든다. 그래야 항상 나누어떨어진다. */
    private static List<Integer> divPair(GradeRange g, Random rnd) {
        int divisor = between(2, 9, rnd);
        int quotient = between(2, g.grade() <= 3 ? 12 : 25, rnd);
        return List.of(divisor * quotient, divisor);
    }

    /** 받아내림은 두 자리 이상에서만 생길 수 있다. */
    static boolean borrowPossible(GradeRange g) {
        return g.max() >= 10;
    }

    /**
     * 받아올림이 '없는' 덧셈은 뽑기만 해서는 거의 안 걸린다.
     * 세 자리 수 세 개를 무작위로 뽑아 모든 자리의 합이 10 미만일 확률은 1%도 안 된다.
     * 그래서 다시 뽑는 대신 자릿수마다 합이 9를 넘지 않게 **만들어 낸다**.
     */
    private static List<Integer> buildWithoutCarry(int count, GradeRange g, Random rnd) {
        int digits = String.valueOf(g.max()).length();
        int leadingMin = Math.max(1, g.min() / (int) Math.pow(10, digits - 1.0));

        int[] values = new int[count];
        for (int column = 0; column < digits; column++) {
            int[] columnDigits = splitUnderTen(count, column == 0 ? leadingMin : 0, rnd);
            for (int i = 0; i < count; i++) {
                values[i] = values[i] * 10 + columnDigits[i];
            }
        }
        return Arrays.stream(values).boxed().toList();
    }

    /** 합이 9 이하가 되도록 자릿수를 나눠 준다. 나눠 주는 순서를 섞어 한쪽만 커지지 않게 한다. */
    private static int[] splitUnderTen(int count, int min, Random rnd) {
        int[] digits = new int[count];
        Arrays.fill(digits, min);

        int slack = 9 - min * count;
        if (slack <= 0) {
            return digits;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            order.add(i);
        }
        Collections.shuffle(order, rnd);
        for (int index : order) {
            if (slack <= 0) {
                break;
            }
            int give = rnd.nextInt(slack + 1);
            digits[index] += give;
            slack -= give;
        }
        return digits;
    }

    // ── 자리 올림·내림 판정 ──

    /** 어느 자리에서든 합이 10 을 넘으면 받아올림이 있다. */
    static boolean hasCarry(List<Integer> numbers) {
        int[] left = numbers.stream().mapToInt(Integer::intValue).toArray();
        while (anyPositive(left)) {
            int columnSum = 0;
            for (int i = 0; i < left.length; i++) {
                columnSum += left[i] % 10;
                left[i] /= 10;
            }
            if (columnSum >= 10) {
                return true;
            }
        }
        return false;
    }

    /** 어느 자리에서든 빼는 쪽이 크면 받아내림이 있다. */
    static boolean hasBorrow(int a, int b) {
        while (b > 0) {
            if (a % 10 < b % 10) {
                return true;
            }
            a /= 10;
            b /= 10;
        }
        return false;
    }

    private static boolean anyPositive(int[] values) {
        for (int v : values) {
            if (v > 0) {
                return true;
            }
        }
        return false;
    }

    private static int between(int min, int max, Random rnd) {
        if (max <= min) {
            return min;
        }
        return min + rnd.nextInt(max - min + 1);
    }
}
