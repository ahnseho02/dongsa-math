package com.dongsa.math.problem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 문제에 나오는 수 하나.
 *
 * 초5~6 과정에는 분수와 소수가 나온다. double 로 다루면 0.1 + 0.2 가 0.30000000000000004 이 되어
 * 아이가 맞게 써도 틀렸다고 나온다. 그래서 속으로는 **항상 분수(분자/분모)** 로 들고 계산하고,
 * 보여 줄 때만 정수·소수·분수 중 어느 모습으로 쓸지 고른다.
 *
 * 분모는 늘 양수이고 기약분수로 줄여 둔다. 그래서 2/4 와 1/2 는 같은 값으로 취급된다.
 */
public final class Num implements Comparable<Num> {

    /** 화면에 어떤 모습으로 쓸지. 값 자체와는 무관하다. */
    public enum Form { INTEGER, DECIMAL, FRACTION }

    public static final Num ZERO = Num.of(0);

    private final long numerator;
    private final long denominator;
    private final Form form;

    private Num(long numerator, long denominator, Form form) {
        if (denominator == 0) {
            throw new ArithmeticException("분모가 0 입니다");
        }
        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }
        long g = gcd(Math.abs(numerator), denominator);
        this.numerator = numerator / g;
        this.denominator = denominator / g;
        // 값이 정수가 되면 분수 모양으로 둘 이유가 없다 (1/2 + 1/2 는 "1")
        this.form = (this.denominator == 1 && form == Form.FRACTION) ? Form.INTEGER : form;
    }

    public static Num of(long value) {
        return new Num(value, 1, Form.INTEGER);
    }

    public static Num fraction(long numerator, long denominator) {
        return new Num(numerator, denominator, Form.FRACTION);
    }

    /** decimal(15, 1) 은 1.5 */
    public static Num decimal(long unscaled, int scale) {
        long power = 1;
        for (int i = 0; i < scale; i++) {
            power *= 10;
        }
        return new Num(unscaled, power, Form.DECIMAL);
    }

    /**
     * 아이가 적은 답을 읽는다. "12", "1.5", "3/4", "1 1/2"(대분수) 를 받는다.
     * 읽을 수 없으면 null — 틀렸다고 처리하면 된다.
     */
    public static Num parse(String text) {
        if (text == null) {
            return null;
        }
        String s = text.strip().replace(" ", "");
        if (s.isEmpty()) {
            return null;
        }
        try {
            // 대분수: 1_1/2 처럼 정수부와 분수부가 붙어 있는 경우는 받지 않는다 (구분이 안 된다)
            int slash = s.indexOf('/');
            if (slash > 0) {
                long n = Long.parseLong(s.substring(0, slash));
                long d = Long.parseLong(s.substring(slash + 1));
                return d == 0 ? null : fraction(n, d);
            }
            if (s.contains(".")) {
                BigDecimal value = new BigDecimal(s);
                return decimal(value.unscaledValue().longValueExact(), value.scale());
            }
            return of(Long.parseLong(s));
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    public Num plus(Num other) {
        return new Num(numerator * other.denominator + other.numerator * denominator,
                denominator * other.denominator, widerForm(other));
    }

    public Num minus(Num other) {
        return new Num(numerator * other.denominator - other.numerator * denominator,
                denominator * other.denominator, widerForm(other));
    }

    public Num times(Num other) {
        return new Num(numerator * other.numerator, denominator * other.denominator, widerForm(other));
    }

    public Num dividedBy(Num other) {
        if (other.numerator == 0) {
            throw new ArithmeticException("0 으로 나눌 수 없습니다");
        }
        return new Num(numerator * other.denominator, denominator * other.numerator, widerForm(other));
    }

    /** 값이 같은가. 모습은 보지 않는다 — 0.5 와 1/2 는 같다. */
    public boolean equalsValue(Num other) {
        return other != null && numerator == other.numerator && denominator == other.denominator;
    }

    public boolean isInteger() {
        return denominator == 1;
    }

    public boolean isNegative() {
        return numerator < 0;
    }

    public Form form() {
        return form;
    }

    public long numerator() {
        return numerator;
    }

    public long denominator() {
        return denominator;
    }

    /** 화면과 문장에 들어갈 모습. */
    public String text() {
        if (denominator == 1) {
            return String.valueOf(numerator);
        }
        if (form == Form.DECIMAL) {
            String decimal = asDecimalText();
            if (decimal != null) {
                return decimal;
            }
            // 1/3 처럼 소수로 딱 떨어지지 않으면 분수로 보여 준다
        }
        return numerator + "/" + denominator;
    }

    /** 소수로 딱 떨어지면 그 글자, 아니면 null. */
    private String asDecimalText() {
        long d = denominator;
        while (d % 2 == 0) { d /= 2; }
        while (d % 5 == 0) { d /= 5; }
        if (d != 1) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 10, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    /** 분수·소수가 섞이면 분수 쪽을 따른다 — 그래야 1/3 같은 값을 잃지 않는다. */
    private Form widerForm(Num other) {
        if (form == Form.FRACTION || other.form == Form.FRACTION) {
            return Form.FRACTION;
        }
        if (form == Form.DECIMAL || other.form == Form.DECIMAL) {
            return Form.DECIMAL;
        }
        return Form.INTEGER;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a == 0 ? 1 : a;
    }

    @Override
    public int compareTo(Num other) {
        return Long.compare(numerator * other.denominator, other.numerator * denominator);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Num n && equalsValue(n);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    @Override
    public String toString() {
        return text();
    }
}
