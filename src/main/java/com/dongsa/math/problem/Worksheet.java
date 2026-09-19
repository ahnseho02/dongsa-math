package com.dongsa.math.problem;

import java.util.List;

/** 만들어진 학습지. code 를 다시 넣으면 같은 학습지가 나온다. */
public record Worksheet(String code, WorksheetSpec spec, List<GeneratedProblem> problems) {
}
