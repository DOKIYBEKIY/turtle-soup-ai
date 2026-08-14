package com.example.turtle_soup_ai.service;

/**
 * AI 服务异常：调用失败、未配置 Key、返回格式异常等场景统一抛出。
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
