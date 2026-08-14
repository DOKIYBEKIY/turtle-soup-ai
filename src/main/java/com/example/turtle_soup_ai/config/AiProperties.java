package com.example.turtle_soup_ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 服务配置项，对应 application.yaml 中的 ai.qwen 前缀。
 * api-key 支持从环境变量 DASHSCOPE_API_KEY 注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.qwen")
public class AiProperties {

    /** 通义千问兼容模式接口地址 */
    private String endpoint;

    /** 模型名称 */
    private String model;

    /** API Key，优先取环境变量 DASHSCOPE_API_KEY */
    private String apiKey;

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 30000;

    /** 采样温度，越低越稳定 */
    private double temperature = 0.1;
}
