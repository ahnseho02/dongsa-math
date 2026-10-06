package com.dongsa.math.game;

import com.dongsa.math.problem.Num;
import com.dongsa.math.problem.Operation;

/** 게임 문제 하나. 문장 없이 숫자와 기호만 있다 — 계산 속도를 보는 것이 목적이다. */
public record GameProblem(int no, Num left, Num right, Operation operation, Num answer) {

    public String expression() {
        return left.text() + " " + operation.sign() + " " + right.text();
    }

    public String answerText() {
        return answer.text();
    }
}
