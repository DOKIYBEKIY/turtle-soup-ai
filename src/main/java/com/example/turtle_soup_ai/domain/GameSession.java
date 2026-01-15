package com.example.turtle_soup_ai.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class GameSession {
    private String id = UUID.randomUUID().toString();
    private String soupSurface;
    private String soupBottom;
    private boolean finished = false;

    private boolean win; // 是否胜利
    private String finalStatement; // 玩家最终陈述
    private List<String> logs = new ArrayList<>(); // 游戏日志列表

    public void addLog(String log) {
        this.logs.add(log);
    }

}
