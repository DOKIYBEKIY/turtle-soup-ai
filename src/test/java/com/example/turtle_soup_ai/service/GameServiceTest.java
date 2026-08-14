package com.example.turtle_soup_ai.service;

import com.example.turtle_soup_ai.domain.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GameService 测试：验证汤题加载、轮换不重复、重置逻辑。
 * 直接操作内存中的 soupPool，不依赖 Spring 容器。
 */
class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService();

        List<GameSession> pool = new ArrayList<>();
        pool.add(session("汤面1", "汤底1"));
        pool.add(session("汤面2", "汤底2"));
        pool.add(session("汤面3", "汤底3"));

        ReflectionTestUtils.setField(gameService, "soupPool", pool);
        gameService.startNewGame();
    }

    private GameSession session(String surface, String bottom) {
        GameSession s = new GameSession();
        s.setSoupSurface(surface);
        s.setSoupBottom(bottom);
        return s;
    }

    @Test
    void 开局应有汤面且进度为1() {
        assertNotNull(gameService.getCurrentSession());
        assertEquals(1, gameService.getPlayedCount());
        assertEquals(3, gameService.getTotalCount());
        assertFalse(gameService.allSoupsPlayed());
    }

    @Test
    void 题目不重复轮换() {
        List<String> seen = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String surface = gameService.getCurrentSession().getSoupSurface();
            assertFalse(seen.contains(surface), "题目不应重复");
            seen.add(surface);
            gameService.startNewGame();
        }
        assertEquals(3, seen.size());
        assertNull(gameService.getCurrentSession(), "全部玩完后当前对局应为空");
        assertTrue(gameService.allSoupsPlayed());
    }

    @Test
    void 全部玩完后重置可重新开始() {
        for (int i = 0; i < 3; i++) {
            gameService.startNewGame();
        }
        assertTrue(gameService.allSoupsPlayed());

        gameService.resetAll();
        assertNotNull(gameService.getCurrentSession());
        assertEquals(1, gameService.getPlayedCount());
        assertFalse(gameService.allSoupsPlayed());
    }

    @Test
    void 胜利应标记对局结束() {
        GameSession current = gameService.getCurrentSession();
        gameService.markWin("我猜到了真相");
        assertTrue(current.isWin());
        assertTrue(current.isFinished());
        assertEquals("我猜到了真相", current.getFinalStatement());
    }

    @Test
    void 无对局时胜利标记应抛异常() {
        for (int i = 0; i < 3; i++) {
            gameService.startNewGame();
        }
        assertNull(gameService.getCurrentSession());
        assertThrows(IllegalStateException.class, () -> gameService.markWin("x"));
    }
}
