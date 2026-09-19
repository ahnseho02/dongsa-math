package com.dongsa.math.problem;

/**
 * 사람이 손으로 옮겨 적는 코드.
 * 아이가 보고 입력하므로 I·O·0·1 처럼 헷갈리는 글자를 뺀 32 글자만 쓴다.
 * 32 글자라서 한 글자가 정확히 5비트다 — 시드를 그대로 코드로 바꿀 수 있다.
 */
public final class ReadableCode {

    private ReadableCode() {}

    public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int LENGTH = 6;

    /** 6글자 = 30비트. 시드는 그 범위 안에서만 쓴다. */
    public static final long MAX_SEED = 1L << (5 * LENGTH);

    public static String encode(long seed) {
        long s = Math.floorMod(seed, MAX_SEED);
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.insert(0, ALPHABET.charAt((int) (s % 32)));
            s /= 32;
        }
        return sb.toString();
    }

    /** 코드가 규칙에 맞지 않으면 null. 호출한 쪽에서 새 시드를 뽑는다. */
    public static Long decode(String code) {
        if (code == null) {
            return null;
        }
        String c = code.strip().toUpperCase();
        if (c.length() != LENGTH) {
            return null;
        }
        long seed = 0;
        for (int i = 0; i < LENGTH; i++) {
            int index = ALPHABET.indexOf(c.charAt(i));
            if (index < 0) {
                return null;
            }
            seed = seed * 32 + index;
        }
        return seed;
    }
}
