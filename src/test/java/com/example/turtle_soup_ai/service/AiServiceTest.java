package com.example.turtle_soup_ai.service;

import com.example.turtle_soup_ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AiService 测试：验证 OpenAI 兼容响应解析与回答规范化，不真正调用 AI。
 */
class AiServiceTest {

    private AiService aiService;
    private RestTemplate restTemplate;
    private AiProperties props;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        props = new AiProperties();
        props.setEndpoint("https://example.com/chat/completions");
        props.setModel("qwen-turbo");
        props.setApiKey("test-key");
        props.setTemperature(0.1);
        aiService = new AiService(restTemplate, props);
    }

    private void mockResponse(String content) {
        Map<String, Object> message = Map.of("content", content);
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> body = Map.of("choices", List.of(choice));
        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                ArgumentMatchers.<Class<Map>>any()
        )).thenReturn(ResponseEntity.ok(body));
    }

    @Test
    void 合法回答直接返回() {
        mockResponse("是");
        var session = new com.example.turtle_soup_ai.domain.GameSession();
        session.setSoupBottom("汤底");
        assertEquals("是", aiService.answer("问题", session));
    }

    @Test
    void 非规范回答规范化为无关() {
        mockResponse("是的，凶手就是邻居");
        var session = new com.example.turtle_soup_ai.domain.GameSession();
        session.setSoupBottom("汤底");
        assertEquals("无关", aiService.answer("问题", session));
    }

    @Test
    void 胜利判定识别() {
        mockResponse("胜利");
        assertTrue(aiService.checkWin("陈述", "汤底"));
    }

    @Test
    void 未胜利判定识别() {
        mockResponse("未胜利");
        assertFalse(aiService.checkWin("陈述", "汤底"));
    }

    @Test
    void 空内容返回null() {
        mockResponse("");
        var session = new com.example.turtle_soup_ai.domain.GameSession();
        session.setSoupBottom("汤底");
        assertEquals("无关", aiService.answer("问题", session));
    }

    @Test
    void 无会话时抛异常() {
        assertThrows(IllegalStateException.class, () -> aiService.answer("问题", null));
    }

    @Test
    void 未配置Key时抛异常() {
        props.setApiKey(null);
        var session = new com.example.turtle_soup_ai.domain.GameSession();
        session.setSoupBottom("汤底");
        assertThrows(AiServiceException.class, () -> aiService.answer("问题", session));
    }

    @Test
    void 提取空响应体返回null() {
        assertNull(aiService.extractContent(null));
        assertNull(aiService.extractContent(Map.of()));
        assertNull(aiService.extractContent(Map.of("choices", List.of())));
    }
}
