package com.dongsa.math.problem;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 템플릿 하나 + 난수 하나 → 문제 하나. 같은 난수를 주면 항상 같은 문제가 나온다. */
@Component
public class ProblemGenerator {

    public GeneratedProblem generate(ProblemTemplate template, GradeRange grade, boolean carry, Random rnd) {
        // 슬롯 값 a, b, c — 문장에 들어가는 수
        List<Num> slots = new ArrayList<>(
                NumberFactory.generate(template.getNumberPattern(), grade, carry, rnd));

        // "한 권에 900원" 처럼 값을 100 단위로 떨어뜨려야 자연스러운 문장이 있다
        if (template.getSecondScale() != 1 && slots.size() > 1) {
            slots.set(1, slots.get(1).times(Num.of(template.getSecondScale())));
        }

        // 식에 들어가는 수는 슬롯 중에서 템플릿이 고른 것. 같은 슬롯을 두 번 쓸 수도 있다.
        List<Integer> indexes = template.operandSlotIndexes();
        List<Num> operands = new ArrayList<>(indexes.size());
        for (int index : indexes) {
            operands.add(slots.get(index));
        }
        // 식에 안 쓰는 슬롯은 함정 숫자다
        List<Num> distractors = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (!indexes.contains(i)) {
                distractors.add(slots.get(i));
            }
        }

        String name = NamePool.pick(rnd);
        String name2 = template.isNeedsTwoNames() ? NamePool.pickOther(name, rnd) : null;

        String sentence = Josa.resolve(fillSlots(template.getPattern(), slots, name, name2));
        Num answer = compute(template.operations(), operands);

        return new GeneratedProblem(
                template.getCode(), template.getCategory(), template.operations(),
                sentence, List.copyOf(template.getCues()),
                List.copyOf(operands), List.copyOf(distractors),
                answer, template.getUnit(), template.isHard());
    }

    /** 한 단계면 그대로, 두 단계면 (첫째 ∘ 둘째) 를 셋째와 묶는다. */
    static Num compute(List<Operation> operations, List<Num> operands) {
        if (operations.size() == 1) {
            return operations.get(0).apply(operands);
        }
        Num first = operations.get(0).apply(operands.subList(0, 2));
        return operations.get(1).apply(List.of(first, operands.get(2)));
    }

    private String fillSlots(String pattern, List<Num> slots, String name, String name2) {
        String out = pattern;
        if (name2 != null) {
            out = out.replace("{name2}", name2);
        }
        out = out.replace("{name}", name);
        String[] keys = {"{a}", "{b}", "{c}"};
        String[] percentKeys = {"{a%}", "{b%}", "{c%}"};
        for (int i = 0; i < slots.size() && i < keys.length; i++) {
            out = out.replace(percentKeys[i], slots.get(i).times(Num.of(100)).text());
            out = out.replace(keys[i], slots.get(i).text());
        }
        return out;
    }
}
