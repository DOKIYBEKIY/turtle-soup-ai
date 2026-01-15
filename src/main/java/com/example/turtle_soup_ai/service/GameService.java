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

    /** 开始新的一局 */
    public void startNewGame() {
        current = soupPool.get(
                ThreadLocalRandom.current().nextInt(soupPool.size())
        );
        current.setFinished(false);
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
}
