package com.dongsa.math.service;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import com.dongsa.math.domain.GameRound;
import com.dongsa.math.domain.Student;
import com.dongsa.math.game.GameFactory;
import com.dongsa.math.game.GameProblem;
import com.dongsa.math.problem.ReadableCode;
import com.dongsa.math.repository.GameRoundRepository;
import com.dongsa.math.repository.StudentRepository;
import com.dongsa.math.security.LoginUser;
import com.dongsa.math.web.dto.GameDtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 연산 게임.
 *
 * 문장제가 "왜 이 식인가" 를 묻는다면, 게임은 순수하게 계산 속도를 올리는 쪽이다.
 * 둘 다 필요하다 — 식은 세우는데 계산에서 틀리는 아이가 실제로 있다.
 *
 * 점수를 꾸미지 못하게 하는 방법은 학습지와 같다. **답을 내려보내지 않고**,
 * 채점할 때 서버가 씨앗으로 문제를 다시 만든다.
 */
@Service
@Transactional(readOnly = true)
public class GameService {

    private static final int RANK_SIZE = 10;

    private final GameRoundRepository rounds;
    private final StudentRepository students;
    private final GameFactory factory;
    private final SecureRandom random = new SecureRandom();

    public GameService(GameRoundRepository rounds, StudentRepository students, GameFactory factory) {
        this.rounds = rounds;
        this.students = students;
        this.factory = factory;
    }

    @Transactional
    public StartResponse start(LoginUser me) {
        Student student = students.findByIdAndAcademyId(me.id(), me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));

        long seed = Math.floorMod(random.nextLong(), ReadableCode.MAX_SEED);
        GameRound round = rounds.save(new GameRound(student, student.getGrade(), seed,
                GameFactory.PROBLEM_COUNT, GameFactory.LIMIT_SECONDS, Instant.now()));

        List<Item> items = factory.build(student.getGrade(), seed).stream()
                .map(p -> new Item(p.no(), p.expression()))
                .toList();
        return new StartResponse(round.getId(), student.getGrade(), GameFactory.LIMIT_SECONDS, items);
    }

    @Transactional
    public FinishResponse finish(LoginUser me, Long roundId, FinishRequest req) {
        GameRound round = rounds.findByIdAndStudentId(roundId, me.id())
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
        if (round.isFinished()) {
            throw new ApiException(ErrorCode.GAME_ALREADY_FINISHED);
        }
        Instant now = Instant.now();
        if (round.isTooLate(now)) {
            round.finish(0, 0, now);
            throw new ApiException(ErrorCode.GAME_TIME_OVER);
        }

        Map<Integer, GameProblem> problems = new HashMap<>();
        factory.build(round.getGrade(), round.getSeed()).forEach(p -> problems.put(p.no(), p));

        int attempted = 0;
        int solved = 0;
        List<Wrong> wrong = new ArrayList<>();
        for (Answer answer : req.answers()) {
            GameProblem problem = problems.get(answer.no());
            if (problem == null || answer.value() == null || answer.value().isBlank()) {
                continue;
            }
            attempted++;
            Integer value = parse(answer.value());
            if (value != null && value == problem.answer()) {
                solved++;
            } else {
                wrong.add(new Wrong(problem.no(), problem.expression(), answer.value().strip(), problem.answer()));
            }
        }

        int previousBest = rounds.bestOf(me.id());
        round.finish(attempted, solved, now);

        return new FinishResponse(
                solved, attempted, round.getProblemCount(),
                (int) Duration.between(round.getStartedAt(), now).getSeconds(),
                Math.max(previousBest, solved), solved > previousBest,
                rankOf(me.academyId(), me.id()),
                wrong);
    }

    /** 순위표. 아이마다 가장 잘한 한 판만 센다. */
    public Ranking ranking(LoginUser me, Long meStudentId) {
        List<Object[]> raw = rounds.ranking(me.academyId(), PageRequest.of(0, RANK_SIZE));
        List<RankRow> rows = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Object[] r = raw.get(i);
            Long studentId = (Long) r[0];
            rows.add(new RankRow(i + 1, studentId, (String) r[1], ((Number) r[2]).intValue(),
                    ((Number) r[3]).intValue(),
                    r[4] == null ? null : ((Number) r[4]).intValue(),
                    ((Number) r[5]).longValue(),
                    studentId.equals(meStudentId)));
        }
        Integer myRank = meStudentId == null ? null : rankOf(me.academyId(), meStudentId);
        return new Ranking(rounds.countPlayers(me.academyId()), myRank,
                meStudentId == null ? 0 : rounds.bestOf(meStudentId),
                meStudentId == null ? 0 : rounds.playCountOf(meStudentId), rows);
    }

    /**
     * 내 순위. 상위 10명 밖이어도 자기 등수는 알려 준다.
     * 아이 수가 학원 단위(많아야 수백)라 전체를 훑어도 부담이 없다.
     */
    private Integer rankOf(Long academyId, Long studentId) {
        List<Object[]> all = rounds.ranking(academyId, PageRequest.of(0, 10_000));
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i)[0].equals(studentId)) {
                return i + 1;
            }
        }
        return null;
    }

    private Integer parse(String value) {
        try {
            return Integer.valueOf(value.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
