package com.dongsa.math.problem;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 템플릿 하나 + 난수 하나 → 문제 하나. 같은 난수를 주면 항상 같은 문제가 나온다. */
@Component
public class ProblemGenerator {

    public GeneratedProblem generate(ProblemTemplate template, GradeRange grade, boolean carry, Random rnd) {
        List<Integer> numbers = new ArrayList<>(
                NumberFactory.generate(template.getNumberPattern(), grade, carry, rnd));

        // "한 권에 900원" 처럼 값을 100 단위로 떨어뜨려야 자연스러운 문장이 있다
        if (template.getSecondScale() != 1 && numbers.size() > 1) {
            numbers.set(1, numbers.get(1) * template.getSecondScale());
        }

        String name = NamePool.pick(rnd);
        String name2 = template.isNeedsTwoNames() ? NamePool.pickOther(name, rnd) : null;

        String sentence = Josa.resolve(fillSlots(template.getPattern(), numbers, name, name2));
        int answer = template.getOperation().apply(numbers);

        return new GeneratedProblem(
                template.getCode(), template.getCategory(), template.getOperation(),
                sentence, List.copyOf(template.getCues()), List.copyOf(numbers),
                answer, template.getUnit());
    }

    private String fillSlots(String pattern, List<Integer> numbers, String name, String name2) {
        String out = pattern;
        // {name2} 를 먼저 바꾼다 — {name} 이 {name2} 의 앞부분과 겹치기 때문이다
        if (name2 != null) {
            out = out.replace("{name2}", name2);
        }
        out = out.replace("{name}", name);
        String[] keys = {"{a}", "{b}", "{c}"};
        for (int i = 0; i < numbers.size() && i < keys.length; i++) {
            out = out.replace(keys[i], String.valueOf(numbers.get(i)));
        }
        return out;
    }
}
