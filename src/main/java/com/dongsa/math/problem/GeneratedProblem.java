package com.dongsa.math.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 만들어진 문제 하나.
 *
 * sentence 에는 «모두» 처럼 아이가 누를 수 있는 단어가 표시된 채로 들어 있다.
 * numbers 는 식에 들어가는 수를 순서대로, distractors 는 문장에만 나오고 식에는 안 쓰는 함정 숫자다.
 * operations 는 한 개(보통) 또는 두 개(두 단계 문제)다.
 */
public record GeneratedProblem(
        String templateCode,
        ProblemCategory category,
        List<Operation> operations,
        String sentence,
        List<String> cues,
        List<Num> numbers,
        List<Num> distractors,
        Num answer,
        String unit,
        boolean hard) {

    private static final Pattern TAPPABLE = Pattern.compile("«([^»]+)»");

    /** 첫 번째(또는 유일한) 연산. */
    public Operation operation() {
        return operations.get(0);
    }

    public boolean isTwoStep() {
        return operations.size() == 2;
    }

    /** 표시를 걷어낸 문장. 종이에 인쇄할 때 쓴다. */
    public String plainSentence() {
        return sentence.replace("«", "").replace("»", "");
    }

    /** 아이가 누를 수 있는 단어들. 이 중 cues 에 든 것이 정답이다. */
    public List<String> tappableWords() {
        Matcher m = TAPPABLE.matcher(sentence);
        return m.results().map(r -> r.group(1)).toList();
    }

    /** "342 + 458", 두 단계면 "(420 − 135) + 420" */
    public String expression() {
        if (isTwoStep()) {
            return "(" + numbers.get(0).text() + " " + operations.get(0).sign() + " " + numbers.get(1).text() + ") "
                    + operations.get(1).sign() + " " + numbers.get(2).text();
        }
        return String.join(" " + operation().sign() + " ", numbers.stream().map(Num::text).toList());
    }

    /** 아이에게 보여 줄 숫자 칩 — 식에 쓰는 수와 함정 숫자를 함께. 섞는 것은 화면 쪽이 한다. */
    public List<String> shownNumberTexts() {
        List<String> out = new ArrayList<>();
        numbers.forEach(n -> out.add(n.text()));
        distractors.forEach(n -> out.add(n.text()));
        return out;
    }

    public List<String> numberTexts() {
        return numbers.stream().map(Num::text).toList();
    }

    public String answerText() {
        return answer.text();
    }

    /** "−" 또는 두 단계면 "−,+". 연산 단계의 정답 값이다. */
    public String operationsText() {
        return String.join(",", operations.stream().map(Operation::sign).toList());
    }

    public boolean isCue(String word) {
        return cues.contains(word);
    }
}
