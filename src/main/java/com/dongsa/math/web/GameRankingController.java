package com.dongsa.math.web;

import com.dongsa.math.security.LoginUser;
import com.dongsa.math.service.GameService;
import com.dongsa.math.web.dto.GameDtos.Ranking;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 선생님이 보는 순위표. 아이들이 얼마나 하고 있는지 확인용. */
@RestController
@RequestMapping("/api/games")
public class GameRankingController {

    private final GameService games;

    public GameRankingController(GameService games) {
        this.games = games;
    }

    @GetMapping("/ranking")
    public Ranking ranking(@AuthenticationPrincipal LoginUser me) {
        return games.ranking(me, null);
    }
}
