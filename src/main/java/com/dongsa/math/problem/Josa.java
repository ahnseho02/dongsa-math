package com.dongsa.math.problem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한국어 조사 자동 처리.
 * 이름이 '민우'면 "민우는", '서윤'이면 "서윤이" — 앞 글자에 받침이 있는지로 갈린다.
 * 템플릿에는 {은/는} 처럼 적어 두고, 이름이 채워진 뒤에 앞 글자를 보고 고른다.
 *
 * 표기 규칙: 앞쪽이 '받침 있을 때', 뒤쪽이 '받침 없을 때'다. ({은/는}, {이/가}, {을/를}, {과/와})
 */
public final class Josa {

    private Josa() {}

    private static final Pattern JOSA = Pattern.compile("\\{([가-힣]+)/([가-힣]+)\\}");

    /** 숫자를 한글로 읽었을 때의 종성. 0 영, 1 일, 2 이, 3 삼 … (8 은 ㄹ 받침) */
    private static final int[] DIGIT_JONG = {1, 8, 0, 1, 0, 0, 1, 8, 8, 0};

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_LAST = 0xD7A3;
    private static final int JONGSEONG_COUNT = 28;
    private static final int RIEUL = 8;

    /** 마지막 글자의 종성 번호. 0 이면 받침이 없다. */
    public static int jongseong(String text) {
        String s = text == null ? "" : text.strip();
        if (s.isEmpty()) {
            return 0;
        }
        char c = s.charAt(s.length() - 1);
        if (c >= HANGUL_BASE && c <= HANGUL_LAST) {
            return (c - HANGUL_BASE) % JONGSEONG_COUNT;
        }
        if (c >= '0' && c <= '9') {
            return DIGIT_JONG[c - '0'];
        }
        return 0;
    }

    public static String pick(String previous, String withJong, String withoutJong) {
        // '으로/로' 만 규칙이 다르다 — ㄹ 받침에서는 '로' 를 쓴다
        if ("으로".equals(withJong)) {
            int j = jongseong(previous);
            return (j == 0 || j == RIEUL) ? "로" : "으로";
        }
        return jongseong(previous) == 0 ? withoutJong : withJong;
    }

    /**
     * 문장 속의 {은/는} 같은 표시를 실제 조사로 바꾼다.
     * 앞에서부터 차례로 처리하면서, 이미 만들어진 부분의 마지막 글자를 보고 고른다.
     */
    public static String resolve(String text) {
        Matcher m = JOSA.matcher(text);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            out.append(text, last, m.start());
            out.append(pick(lastMeaningfulChar(out), m.group(1), m.group(2)));
            last = m.end();
        }
        out.append(text.substring(last));
        return out.toString();
    }

    /** 탭할 수 있는 단어를 감싸는 «» 와 공백은 건너뛰고 실제 마지막 글자를 찾는다. */
    private static String lastMeaningfulChar(CharSequence built) {
        for (int i = built.length() - 1; i >= 0; i--) {
            char c = built.charAt(i);
            if (c != '«' && c != '»' && !Character.isWhitespace(c)) {
                return String.valueOf(c);
            }
        }
        return "";
    }
}
