package com.example.turtle_soup_ai.web;

/**
 * 当前对局视图：返回给前端的汤面与进度信息。
 */
public record SessionView(String surface, boolean allPlayed, int played, int total) {
}
