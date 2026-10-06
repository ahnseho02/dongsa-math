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

    public static List<Num> generate(NumberPattern pattern, GradeRange grade, boolean carry, Random rnd) {
        return switch (pattern) {
            case ADD_PAIR -> addPair(grade, carry, rnd);
            case ADD_SMALL_SECOND -> addSmallSecond(grade, carry, rnd);
            case SUB_PAIR -> subPair(grade, carry, rnd);
            case ADD_TRIPLE -> addTriple(grade, carry, rnd);
            case MUL_PAIR -> mulPair(grade, rnd);
            case DIV_PAIR -> divPair(grade, rnd);
            case DECIMAL_ADD_PAIR -> decimalPair(grade, rnd, true);
            case DECIMAL_SUB_PAIR -> decimalPair(grade, rnd, false);
            case DECIMAL_MUL_WHOLE -> decimalTimesWhole(grade, rnd);
            case DECIMAL_DIV_WHOLE -> decimalDividedByWhole(rnd);
            case DECIMAL_ADD_TRIPLE -> decimalTriple(grade, rnd);
            case FRACTION_ADD_SAME -> sameDenominator(rnd, true);
            case FRACTION_SUB_SAME -> sameDenominator(rnd, false);
            case FRACTION_ADD_DIFF -> differentDenominator(rnd, true);
            case FRACTION_SUB_DIFF -> differentDenominator(rnd, false);
            case FRACTION_MUL -> List.of(properFraction(rnd), properFraction(rnd));
            case FRACTION_DIV -> fractionDivide(rnd);
            case PERCENT_OF -> percentOf(rnd);
        };
    }

    // ── 소수 (초4~6) ──

    /** 초4 는 소수 첫째 자리까지, 초5 부터 둘째 자리까지 다룬다. */
    private static int decimalScale(GradeRange g, Random rnd) {
        return g.grade() <= 4 ? 1 : (rnd.nextBoolean() ? 1 : 2);
    }

    private static List<Num> decimalPair(GradeRange g, Random rnd, boolean add) {
        int scale = decimalScale(g, rnd);
        int unit = (int) Math.pow(10, scale);
        int max = 99 * unit / 10;            // 초4 면 99.0, 초5 면 99.00 근처
        int a = between(unit / 2, max, rnd);
        int b = add ? between(unit / 2, max, rnd) : between(1, a - 1, rnd);
        return List.of(Num.decimal(a, scale), Num.decimal(b, scale));
    }

    private static List<Num> decimalTriple(GradeRange g, Random rnd) {
        int scale = decimalScale(g, rnd);
        int unit = (int) Math.pow(10, scale);
        int max = 20 * unit;                                  // 가방 무게라 20kg 쯤까지
        return List.of(Num.decimal(between(unit / 10 + 1, max, rnd), scale),
                       Num.decimal(between(unit / 10 + 1, max, rnd), scale),
                       Num.decimal(between(unit / 10 + 1, max, rnd), scale));
    }

    /** (소수) × (자연수) — 0.75 × 4 처럼. */
    private static List<Num> decimalTimesWhole(GradeRange g, Random rnd) {
        int scale = decimalScale(g, rnd);
        int unit = (int) Math.pow(10, scale);
        return List.of(Num.decimal(between(unit / 10 + 1, 99 * unit / 10, rnd), scale),
                       Num.of(between(2, 9, rnd)));
    }

    /**
     * (소수) ÷ (자연수) — 몫을 먼저 정하고 거꾸로 곱해서 나눠지는 수를 만든다.
     * 그래야 4.8 ÷ 6 = 0.8 처럼 딱 떨어진다.
     */
    private static List<Num> decimalDividedByWhole(Random rnd) {
        for (int i = 0; i < MAX_TRIES; i++) {
            int divisor = between(2, 9, rnd);
            Num quotient = Num.decimal(between(2, 95, rnd), 1);   // 0.2 ~ 9.5
            Num dividend = quotient.times(Num.of(divisor));
            // 5.5 × 8 = 44 처럼 나눠지는 수가 정수가 되면 소수 나눗셈 문제가 아니게 된다
            if (!dividend.isInteger()) {
                return List.of(dividend, Num.of(divisor));
            }
        }
        return List.of(Num.decimal(48, 1), Num.of(6));            // 4.8 ÷ 6 = 0.8
    }

    // ── 분수 (초4~6) ──

    /** 초등에서 자주 쓰는 분모만 쓴다. 7, 9, 11 같은 분모는 통분이 지저분해진다. */
    private static final int[] DENOMINATORS = {2, 3, 4, 5, 6, 8, 10, 12};

    private static Num properFraction(Random rnd) {
        int den = DENOMINATORS[rnd.nextInt(DENOMINATORS.length)];
        return Num.fraction(between(1, den - 1, rnd), den);
    }

    /** 분모가 같은 덧셈·뺄셈. 합이 1 을 넘지 않게 해서 답이 진분수로 남는다. */
    private static List<Num> sameDenominator(Random rnd, boolean add) {
        for (int i = 0; i < MAX_TRIES; i++) {
            int den = DENOMINATORS[rnd.nextInt(DENOMINATORS.length)];
            if (den < 3) {
                continue;
            }
            int a = between(1, den - 1, rnd);
            int b = between(1, den - 1, rnd);
            if (add && a + b < den) {
                return List.of(Num.fraction(a, den), Num.fraction(b, den));
            }
            if (!add && a > b) {
                return List.of(Num.fraction(a, den), Num.fraction(b, den));
            }
        }
        return add ? List.of(Num.fraction(1, 4), Num.fraction(1, 4))
                   : List.of(Num.fraction(3, 4), Num.fraction(1, 4));
    }

    /** 분모가 다른 덧셈·뺄셈 — 통분해야 푼다. */
    private static List<Num> differentDenominator(Random rnd, boolean add) {
        for (int i = 0; i < MAX_TRIES; i++) {
            Num a = properFraction(rnd);
            Num b = properFraction(rnd);
            if (a.denominator() == b.denominator()) {
                continue;
            }
            if (add && a.plus(b).compareTo(Num.of(1)) <= 0) {
                return List.of(a, b);
            }
            if (!add && a.compareTo(b) > 0) {
                return List.of(a, b);
            }
        }
        return add ? List.of(Num.fraction(1, 2), Num.fraction(1, 3))
                   : List.of(Num.fraction(2, 3), Num.fraction(1, 4));
    }

    private static List<Num> fractionDivide(Random rnd) {
        Num a = properFraction(rnd);
        Num b = properFraction(rnd);
        return List.of(a, b);
    }

    // ── 비율 (초6) ──

    private static final int[] PERCENTS = {5, 10, 15, 20, 25, 30, 40, 50, 60, 75, 80};

    /** 전체의 몇 퍼센트. 식은 (전체) × (소수로 고친 비율). 답이 정수로 떨어지는 것만 쓴다. */
    private static List<Num> percentOf(Random rnd) {
        for (int i = 0; i < MAX_TRIES; i++) {
            int percent = PERCENTS[rnd.nextInt(PERCENTS.length)];
            int total = between(2, 60, rnd) * 100;           // 200 ~ 6000
            if (total * percent % 100 == 0) {
                return List.of(Num.of(total), Num.decimal(percent, 2));
            }
        }
        return List.of(Num.of(200), Num.decimal(50, 2));
    }

    private static List<Num> addPair(GradeRange g, boolean carry, Random rnd) {
        if (!carry) {
            return buildWithoutCarry(2, g, rnd);
        }
        List<Num> last = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            last = ints(between(g.min(), g.max(), rnd), between(g.min(), g.max(), rnd));
            if (hasCarry(last)) {
                return last;
            }
        }
        return last;
    }

    private static List<Num> addSmallSecond(GradeRange g, boolean carry, Random rnd) {
        int smallMax = Math.max(2, g.max() / 5);
        int smallMin = Math.max(2, smallMax / 5);
        List<Num> last = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            last = ints(between(g.min(), g.max(), rnd), between(smallMin, smallMax, rnd));
            if (hasCarry(last) == carry) {
                return last;
            }
        }
        return last;
    }

    private static List<Num> subPair(GradeRange g, boolean borrow, Random rnd) {
        // 한 자리 수끼리는 받아내림이 생길 수 없다 (앞의 수가 항상 더 크므로).
        // 초1에서 '받아내림 있음'을 골라도 조용히 없는 문제를 낸다 — 수학적으로 불가능하기 때문이다.
        boolean wanted = borrow && borrowPossible(g);
        List<Num> last = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            int a = between(g.min() + 1, g.max(), rnd);
            int b = between(g.min(), a - 1, rnd);
            last = ints(a, b);
            if (hasBorrow(a, b) == wanted) {
                return last;
            }
        }
        return last;
    }

    private static List<Num> addTriple(GradeRange g, boolean carry, Random rnd) {
        if (!carry) {
            return buildWithoutCarry(3, g, rnd);
        }
        List<Num> last = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            last = ints(between(g.min(), g.max(), rnd),
                        between(g.min(), g.max(), rnd),
                        between(g.min(), g.max(), rnd));
            if (hasCarry(last)) {
                return last;
            }
        }
        return last;
    }

    /** 곱셈은 자릿수 대신 구구단 범위로 조절한다. 다섯 자리 × 아홉은 초등 과정이 아니다. */
    private static List<Num> mulPair(GradeRange g, Random rnd) {
        // 초1~2 는 구구단, 초3 은 두 자리까지, 초4 부터 (두 자리)×(한 자리)
        int groupMax = g.grade() <= 2 ? 9 : (g.grade() <= 3 ? 19 : 99);
        return ints(between(2, groupMax, rnd), between(2, 9, rnd));
    }

    /** 몫과 나누는 수를 먼저 정하고 곱해서 전체를 만든다. 그래야 항상 나누어떨어진다. */
    private static List<Num> divPair(GradeRange g, Random rnd) {
        int divisor = between(2, 9, rnd);
        int quotient = between(2, g.grade() <= 3 ? 12 : 25, rnd);
        return ints(divisor * quotient, divisor);
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
    private static List<Num> buildWithoutCarry(int count, GradeRange g, Random rnd) {
        int digits = String.valueOf(g.max()).length();
        int leadingMin = Math.max(1, g.min() / (int) Math.pow(10, digits - 1.0));

        int[] values = new int[count];
        for (int column = 0; column < digits; column++) {
            int[] columnDigits = splitUnderTen(count, column == 0 ? leadingMin : 0, rnd);
            for (int i = 0; i < count; i++) {
                values[i] = values[i] * 10 + columnDigits[i];
            }
        }
        return Arrays.stream(values).mapToObj(Num::of).toList();
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

    private static List<Num> ints(long... values) {
        return Arrays.stream(values).mapToObj(Num::of).toList();
    }

    /** 어느 자리에서든 합이 10 을 넘으면 받아올림이 있다. 정수일 때만 뜻이 있다. */
    static boolean hasCarry(List<Num> numbers) {
        int[] left = numbers.stream().mapToInt(n -> (int) n.numerator()).toArray();
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
