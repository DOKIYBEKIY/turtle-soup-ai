package com.example.turtle_soup_ai.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 应用配置：提供带超时设置的 RestTemplate，避免调用 AI 接口时无限等待。
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, AiProperties props) {
        return builder
                .setConnectTimeout(Duration.ofMillis(props.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(props.getReadTimeout()))
                .build();
    }
}
