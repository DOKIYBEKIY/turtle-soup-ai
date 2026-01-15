package com.example.turtle_soup_ai.service;

import com.example.turtle_soup_ai.domain.GameSession;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameService {

    private final List<GameSession> soupPool = new ArrayList<>();
    private GameSession current;

    @PostConstruct
    public void loadSoups() throws IOException {
        Resource resource = new ClassPathResource("soups.txt");
        List<String> lines = Files.readAllLines(resource.getFile().toPath());

        String surface = null;
        String bottom = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("【汤面】")) {
                surface = line.substring("【汤面】".length()).trim();
            } else if (line.startsWith("【汤底】")) {
                bottom = line.substring("【汤底】".length()).trim();

                if (surface != null) {
                    GameSession session = new GameSession();
                    session.setSoupSurface(surface);
                    session.setSoupBottom(bottom);
                    soupPool.add(session);
                }

                surface = null;
                bottom = null;
            }
        }

        if (soupPool.isEmpty()) {
            throw new IllegalStateException("未加载到任何海龟汤，请检查 soups.txt");
        }

        startNewGame();
    }

    private List<GameSession> unplayedSoups() {
        return soupPool.stream()
                .filter(s -> !s.isPlayed()) // 过滤出未被玩过的汤
                .toList();
    }

    /** 开始新的一局 */
    public void startNewGame() {
        List<GameSession> candidates = unplayedSoups();

        // 所有汤都玩完了
        if (candidates.isEmpty()) {
            current = null;
            return;
        }

        // 随机选一个未玩过的汤
        GameSession selected = candidates.get(
                ThreadLocalRandom.current().nextInt(candidates.size())
        );

        // 标记该汤已被玩过
        selected.setPlayed(true);
        // 重置游戏状态
        selected.setFinished(false);
        selected.setWin(false);
        selected.setFinalStatement(null);
        selected.getLogs().clear();

        current = selected;
    }

    public boolean allSoupsPlayed() {
        return unplayedSoups().isEmpty();
    }

    public GameSession getCurrentSession() {
        return current;
    }

    /** 放弃或猜中 */
    public void finishGame() {
        current.setFinished(true);
    }

    public void markWin(String finalStatement) {
        current.setWin(true); // 这里变量名是current，和你原来的保持一致
        current.setFinalStatement(finalStatement);
        current.addLog("[WIN] 玩家胜利");
    }

    public void logGuess(String statement) {
        current.addLog("[GUESS] " + statement);
    }

    public void resetAll() {
        // 同时重置每个汤的played字段（适配你之前的GameSession逻辑）
        soupPool.forEach(soup -> soup.setPlayed(false));
        // 开始新的一局
        startNewGame();
    }
}