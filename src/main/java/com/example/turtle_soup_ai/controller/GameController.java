package com.example.turtle_soup_ai.controller;

import com.example.turtle_soup_ai.domain.GameSession;
import com.example.turtle_soup_ai.service.AiService;
import com.example.turtle_soup_ai.service.GameService;
import com.example.turtle_soup_ai.web.ApiResponse;
import com.example.turtle_soup_ai.web.SessionView;
import org.springframework.web.bind.annotation.*;

/**
 * 游戏接口：统一返回 ApiResponse 结构。
 */
@RestController
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;
    private final AiService aiService;

    public GameController(GameService gameService, AiService aiService) {
        this.gameService = gameService;
        this.aiService = aiService;
    }

    /** 获取当前对局的汤面与进度 */
    @GetMapping("/start")
    public ApiResponse<SessionView> start() {
        return ApiResponse.ok(sessionView());
    }

    /** 玩家提问 */
    @PostMapping("/ask")
    public ApiResponse<String> ask(@RequestParam String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        GameSession session = gameService.getCurrentSession();
        String answer = aiService.answer(question, session);
        gameService.logAsk(question, answer);
        return ApiResponse.ok(answer);
    }

    /** 放弃：返回汤底 */
    @PostMapping("/giveup")
    public ApiResponse<String> giveUp() {
        GameSession session = gameService.getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("当前没有进行中的对局");
        }
        gameService.logGiveUp();
        gameService.finishGame();
        return ApiResponse.ok(session.getSoupBottom());
    }

    /** 新的一局 */
    @PostMapping("/new")
    public ApiResponse<SessionView> newGame() {
        gameService.startNewGame();
        return ApiResponse.ok(sessionView());
    }

    /** 玩家猜测（提交最终陈述） */
    @PostMapping("/guess")
    public ApiResponse<Boolean> guess(@RequestParam String statement) {
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("猜测内容不能为空");
        }
        GameSession session = gameService.getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("当前没有进行中的对局");
        }

        boolean win = aiService.checkWin(statement, session.getSoupBottom());
        if (win) {
            gameService.markWin(statement);
        } else {
            gameService.logGuess(statement);
        }
        return ApiResponse.ok(win);
    }

    /** 重置题库 */
    @PostMapping("/reset")
    public ApiResponse<Void> reset() {
        gameService.resetAll();
        return ApiResponse.ok(null);
    }

    private SessionView sessionView() {
        GameSession session = gameService.getCurrentSession();
        if (session == null) {
            return new SessionView(null, true, gameService.getPlayedCount(), gameService.getTotalCount());
        }
        return new SessionView(
                session.getSoupSurface(),
                false,
                gameService.getPlayedCount(),
                gameService.getTotalCount()
        );
    }
}
