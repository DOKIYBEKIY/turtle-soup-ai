package com.example.turtle_soup_ai.service;

import com.example.turtle_soup_ai.domain.GameSession;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 游戏服务：负责汤题加载、对局状态管理与题目轮换。
 */
@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final List<GameSession> soupPool = new ArrayList<>();
    private GameSession current;

    @PostConstruct
    public void loadSoups() {
        List<String> lines = readSoupsFile();

        String surface = null;
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("【汤面】")) {
                surface = line.substring("【汤面】".length()).trim();
            } else if (line.startsWith("【汤底】")) {
                String bottom = line.substring("【汤底】".length()).trim();
                if (surface != null && !surface.isEmpty() && !bottom.isEmpty()) {
                    GameSession session = new GameSession();
                    session.setSoupSurface(surface);
                    session.setSoupBottom(bottom);
                    soupPool.add(session);
                }
                surface = null;
            }
        }

        if (soupPool.isEmpty()) {
            throw new IllegalStateException("未加载到任何海龟汤，请检查 soups.txt 文件");
        }

        log.info("成功加载 {} 道海龟汤", soupPool.size());
        startNewGame();
    }

    /**
     * 读取 classpath 下的 soups.txt，兼容 jar 打包（使用 getInputStream 而非 getFile）。
     */
    private List<String> readSoupsFile() {
        ClassPathResource resource = new ClassPathResource("soups.txt");
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法读取 soups.txt: " + e.getMessage(), e);
        }
        return lines;
    }

    private List<GameSession> unplayedSoups() {
        return soupPool.stream()
                .filter(s -> !s.isPlayed())
                .toList();
    }

    /** 开始新的一局：从未玩过的汤中随机选一道。全部玩完则 current 置空。 */
    public void startNewGame() {
        List<GameSession> candidates = unplayedSoups();

        if (candidates.isEmpty()) {
            current = null;
            return;
        }

        GameSession selected = candidates.get(
                ThreadLocalRandom.current().nextInt(candidates.size())
        );

        selected.setPlayed(true);
        selected.setFinished(false);
        selected.setWin(false);
        selected.setFinalStatement(null);
        selected.getLogs().clear();

        current = selected;
        log.info("新的一局开始，题目：{}", selected.getSoupSurface());
    }

    public boolean allSoupsPlayed() {
        return unplayedSoups().isEmpty();
    }

    public GameSession getCurrentSession() {
        return current;
    }

    public int getPlayedCount() {
        return (int) soupPool.stream().filter(GameSession::isPlayed).count();
    }

    public int getTotalCount() {
        return soupPool.size();
    }

    /** 放弃或猜中后标记本局结束 */
    public void finishGame() {
        if (current != null) {
            current.setFinished(true);
        }
    }

    public void markWin(String finalStatement) {
        if (current == null) {
            throw new IllegalStateException("当前没有进行中的对局");
        }
        current.setWin(true);
        current.setFinalStatement(finalStatement);
        current.setFinished(true);
        current.addLog("[WIN] 玩家胜利");
    }

    public void logGuess(String statement) {
        if (current == null) {
            throw new IllegalStateException("当前没有进行中的对局");
        }
        current.addLog("[GUESS] " + statement);
    }

    public void logAsk(String question, String answer) {
        if (current == null) {
            throw new IllegalStateException("当前没有进行中的对局");
        }
        current.addLog("[Q] " + question + " -> " + answer);
    }

    public void logGiveUp() {
        if (current != null) {
            current.addLog("[GIVEUP] 玩家放弃，汤底：" + current.getSoupBottom());
        }
    }

    /** 重置全部题目，重新开始 */
    public void resetAll() {
        soupPool.forEach(soup -> soup.setPlayed(false));
        log.info("题库已重置，共 {} 道汤", soupPool.size());
        startNewGame();
    }
}
