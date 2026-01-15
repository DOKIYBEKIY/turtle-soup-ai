package com.example.turtle_soup_ai.service;

import com.example.turtle_soup_ai.domain.GameSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${ai.qwen.endpoint}")
    private String endpoint;

    @Value("${ai.qwen.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String answer(String question, GameSession session) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未设置 DASHSCOPE_API_KEY 环境变量");
        }

        String systemPrompt = """
            你是一个严格的海龟汤主持人。
            你已经知道完整汤底，但不能直接说出来。
            规则：
            - 只能回答：是 / 否 / 无关
            - 不允许解释
            - 不允许扩展
            """;

        String userPrompt = """
            【汤底】
            %s

            玩家问题：
            %s
            """.formatted(session.getSoupBottom(), question);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.1
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    endpoint,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<?, ?> respBody = response.getBody();
            if (respBody == null) return "无关";

            Object choicesObj = respBody.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                return "无关";
            }

            Object messageObj = ((Map<?, ?>) choices.get(0)).get("message");
            if (!(messageObj instanceof Map<?, ?> message)) {
                return "无关";
            }

            String content = String.valueOf(message.get("content")).trim();
            return List.of("是", "否", "无关").contains(content) ? content : "无关";

        } catch (Exception e) {
            return "无关";
        }
    }

    public boolean checkWin(String playerStatement, String soupBase) {
        // 1. 构建判定用的Prompt
        String prompt = """
        你是一个海龟汤裁判。
        【汤底】
        %s
        
        【玩家陈述】
        %s
        
        【判定规则】
        - 不要求措辞完全一致
        - 只要揭示了主要事实和因果关系即可
        - 如果缺失关键原因或只是部分猜测，则判定为未胜利
        
        你只能回答以下之一：
        - 胜利
        - 未胜利
        """.formatted(soupBase, playerStatement);

        // 2. 复用你现有的AI调用逻辑（直接用answer方法的HTTP请求逻辑）
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未设置 DASHSCOPE_API_KEY 环境变量");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 构建请求体（和answer方法的格式一致）
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1
        );

        try {
            // 发送请求并获取AI回复
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    endpoint,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<?, ?> respBody = response.getBody();
            if (respBody == null) return false;

            Object choicesObj = respBody.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                return false;
            }

            Object messageObj = ((Map<?, ?>) choices.get(0)).get("message");
            if (!(messageObj instanceof Map<?, ?> message)) {
                return false;
            }

            String aiReply = String.valueOf(message.get("content")).trim();
            return "胜利".equals(aiReply);

        } catch (Exception e) {
            return false;
        }
    }
}
