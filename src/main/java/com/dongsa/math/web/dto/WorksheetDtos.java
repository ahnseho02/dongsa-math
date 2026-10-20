package com.dongsa.math.web.dto;

import com.dongsa.math.problem.GeneratedProblem;
import com.dongsa.math.problem.ProblemCategory;
import com.dongsa.math.problem.Worksheet;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class WorksheetDtos {

    private WorksheetDtos() {}

    /** code 를 넣으면 그때 나왔던 학습지가 그대로 다시 나온다. 비우면 새로 뽑는다. */
    public record GenerateRequest(
            @NotNull @Min(1) @Max(6) Integer grade,
            @NotNull List<ProblemCategory> categories,
            @Min(1) @Max(50) Integer count,
            Boolean carry,
            Boolean challenge,
            String code) {

        public int countOrDefault() { return count == null ? 10 : count; }

        public boolean carryOrDefault() { return carry == null || carry; }

        /** 기본은 끔 — 도전 문제는 선생님이 고를 때만. */
        public boolean challengeOrDefault() { return Boolean.TRUE.equals(challenge); }
    }

    public record CategoryOption(String name, String label, String operation) {}

    public record GradeOptions(int grade, String numberRange, List<CategoryOption> categories) {}

    /** 선생님이 보는 학습지. 정답과 단서가 들어 있다. */
    public record WorksheetResponse(String code, int grade, boolean carry, boolean challenge, int count,
                                    List<Item> problems) {

        public static WorksheetResponse of(Worksheet w) {
            List<Item> items = new java.util.ArrayList<>();
            for (int i = 0; i < w.problems().size(); i++) {
                items.add(Item.of(i + 1, w.problems().get(i)));
            }
            return new WorksheetResponse(w.code(), w.spec().grade(), w.spec().carry(), w.spec().challenge(),
                    items.size(), items);
        }
    }

    public record Item(
            int no,
            String templateCode,
            String category,
            String categoryLabel,
            /** "−" 또는 두 단계면 "−,+" */
            String operation,
            String sentence,
            String plainSentence,
            List<String> tappableWords,
            List<String> cues,
            /** 식에 들어가는 수, 순서대로 */
            List<String> numbers,
            /** 문장에만 나오고 식에는 안 쓰는 함정 숫자 */
            List<String> distractors,
            String expression,
            String answer,
            String unit,
            boolean hard) {

        static Item of(int no, GeneratedProblem p) {
            return new Item(no, p.templateCode(), p.category().name(), p.category().label(),
                    p.operationsText(), p.sentence(), p.plainSentence(), p.tappableWords(),
                    p.cues(), p.numberTexts(),
                    p.distractors().stream().map(com.dongsa.math.problem.Num::text).toList(),
                    p.expression(), p.answerText(), p.unit(), p.hard());
        }
    }
}
