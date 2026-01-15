package com.example.turtle_soup_ai.controller;

import com.example.turtle_soup_ai.domain.GameSession;
import com.example.turtle_soup_ai.service.AiService;
import com.example.turtle_soup_ai.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;
    private final AiService aiService;
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    public GameController(GameService gameService, AiService aiService) {
        this.gameService = gameService;
        this.aiService = aiService;
    }

    @GetMapping("/start")
    public ResponseEntity<String> start() {
        if (gameService.getCurrentSession() == null) {
            return ResponseEntity.ok("🐢 你已经喝完了所有海龟汤！");
        }
        return ResponseEntity.ok(gameService.getCurrentSession().getSoupSurface());
    }

    @PostMapping("/ask")
    public String ask(@RequestParam String question) {
        String answer = aiService.answer(question, gameService.getCurrentSession());
        logger.info("Q: {} -> A: {}", question, answer);
        return answer;
    }

    @PostMapping("/giveup")
    public String giveUp() {
        gameService.finishGame();
        String bottom = gameService.getCurrentSession().getSoupBottom();
        logger.info("玩家放弃，汤底：{}", bottom);
        return bottom;
    }

    @PostMapping("/new")
    public ResponseEntity<String> newGame() {
        gameService.startNewGame();
        logger.info("开始新的一局");

        if (gameService.getCurrentSession() == null) {
            return ResponseEntity.ok("🐢 你已经喝完了所有海龟汤！");
        }
        return ResponseEntity.ok(gameService.getCurrentSession().getSoupSurface());
    }

    @PostMapping("/guess")
    public ResponseEntity<String> guess(@RequestParam String statement) {
        GameSession session = gameService.getCurrentSession();
        boolean win = aiService.checkWin(statement, session.getSoupBottom()); // 注意：这里应该是getSoupBottom()（你的GameSession里是这个方法）

        if (win) {
            gameService.markWin(statement);
            return ResponseEntity.ok("WIN");
        } else {
            gameService.logGuess(statement);
            return ResponseEntity.ok("NOT_YET");
        }

    }

    //重置题库接口
    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        gameService.resetAll();
        return ResponseEntity.ok().build();
    }
}