package com.dongsa.math.web;

import com.dongsa.math.security.LoginUser;
import com.dongsa.math.service.GameService;
import com.dongsa.math.web.dto.GameDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 아이가 하는 연산 게임. 응답에 정답은 들어 있지 않다. */
@RestController
@RequestMapping("/api/my/games")
public class MyGameController {

    private final GameService games;

    public MyGameController(GameService games) {
        this.games = games;
    }

    @PostMapping
    public ResponseEntity<StartResponse> start(@AuthenticationPrincipal LoginUser me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(games.start(me));
    }

    /** 시간이 끝나면 푼 것을 한 번에 보낸다. 채점은 서버가 한다. */
    @PostMapping("/{id}/finish")
    public FinishResponse finish(@AuthenticationPrincipal LoginUser me,
                                 @PathVariable Long id,
                                 @Valid @RequestBody FinishRequest req) {
        return games.finish(me, id, req);
    }

    @GetMapping("/ranking")
    public Ranking ranking(@AuthenticationPrincipal LoginUser me) {
        return games.ranking(me, me.id());
    }
}
