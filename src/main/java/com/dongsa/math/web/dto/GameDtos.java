package com.dongsa.math.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class GameDtos {

    private GameDtos() {}

    /** 게임 시작. 문제는 오지만 **답은 오지 않는다.** */
    public record StartResponse(
            Long roundId,
            int grade,
            int limitSeconds,
            List<Item> problems) {}

    public record Item(int no, String expression) {}

    public record FinishRequest(@NotNull @Valid List<Answer> answers) {}

    public record Answer(@NotNull @Min(1) Integer no, String value) {}

    public record FinishResponse(
            int solved,
            int attempted,
            int problemCount,
            int elapsedSeconds,
            int best,
            boolean newBest,
            Integer rank,
            List<Wrong> wrong) {}

    /** 끝난 뒤에만 정답을 알려 준다. 틀린 것만 보여 주면 충분하다. */
    public record Wrong(int no, String expression, String submitted, int answer) {}

    public record RankRow(
            int rank,
            Long studentId,
            String studentName,
            int grade,
            int best,
            Integer elapsedMs,
            long plays,
            boolean me) {}

    public record Ranking(long players, Integer myRank, int myBest, long myPlays, List<RankRow> rows) {}
}
