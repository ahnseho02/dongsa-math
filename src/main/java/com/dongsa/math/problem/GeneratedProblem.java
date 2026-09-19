package com.dongsa.math.problem;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 만들어진 문제 하나.
 * sentence 에는 «모두» 처럼 아이가 누를 수 있는 단어가 표시된 채로 들어 있다.
 */
public record GeneratedProblem(
        String templateCode,
        ProblemCategory category,
        Operation operation,
        String sentence,
        List<String> cues,
        List<Integer> numbers,
        int answer,
        String unit) {

    private static final Pattern TAPPABLE = Pattern.compile("«([^»]+)»");

    /** 표시를 걷어낸 문장. 종이에 인쇄할 때 쓴다. */
    public String plainSentence() {
        return sentence.replace("«", "").replace("»", "");
    }

    /** 아이가 누를 수 있는 단어들. 이 중 cues 에 든 것이 정답이다. */
    public List<String> tappableWords() {
        Matcher m = TAPPABLE.matcher(sentence);
        return m.results().map(r -> r.group(1)).toList();
    }

    /** "342 + 458" */
    public String expression() {
        return String.join(" " + operation.sign() + " ", numbers.stream().map(String::valueOf).toList());
    }

    public boolean isCue(String word) {
        return cues.contains(word);
    }
}
