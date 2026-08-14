package com.example.turtle_soup_ai.service;

import com.example.turtle_soup_ai.config.AiProperties;
import com.example.turtle_soup_ai.domain.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 服务：调用通义千问兼容接口，负责问答判定与胜利判定。
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private static final List<String> VALID_ANSWERS = List.of("是", "否", "无关");

    private final RestTemplate restTemplate;
    private final AiProperties props;

    public AiService(RestTemplate restTemplate, AiProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    /**
     * 玩家提问判定：返回「是 / 否 / 无关」。
     */
    public String answer(String question, GameSession session) {
        if (session == null) {
            throw new IllegalStateException("当前没有进行中的对局，请先开始新的一局");
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

        String content = call(systemPrompt, userPrompt);
        return VALID_ANSWERS.contains(content) ? content : "无关";
    }

    /**
     * 胜利判定：玩家陈述是否揭示了核心事实与因果关系。
     */
    public boolean checkWin(String playerStatement, String soupBase) {
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

        String content = call(null, prompt);
        return "胜利".equals(content);
    }

    /**
     * 统一的底层调用：发送 messages，返回 AI 文本内容。
     * system 为 null 时仅发送 user 消息。
     */
    private String call(String system, String user) {
        String apiKey = props.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException("未配置 AI API Key，请设置环境变量 DASHSCOPE_API_KEY 或 ai.qwen.api-key");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (system != null && !system.isBlank()) {
            messages.add(Map.of("role", "system", "content", system));
        }
        messages.add(Map.of("role", "user", "content", user));

        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", messages,
                "temperature", props.getTemperature()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    props.getEndpoint(),
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            String content = extractContent(response.getBody());
            if (content == null) {
                throw new AiServiceException("AI 返回内容为空");
            }
            return content;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 AI 接口失败: {}", e.getMessage());
            throw new AiServiceException("AI 服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 从 OpenAI 兼容响应中提取 message.content 文本。
     */
    String extractContent(Map<?, ?> respBody) {
        if (respBody == null) {
            return null;
        }
        Object choicesObj = respBody.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object messageObj = choice.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return null;
        }
        Object content = message.get("content");
        if (content == null) {
            return null;
        }
        return String.valueOf(content).trim();
    }
}
