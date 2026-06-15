package com.community.athenixback.match.entity;

public enum ArrowStyle {
    SOLID,   // 슈팅
    DASHED,  // 패스
    WAVY;    // 드리블

    public static ArrowStyle fromPlayType(String type) {
        if (type == null) return SOLID;
        String lower = type.toLowerCase();
        if (lower.contains("pass") || lower.contains("cross")) return DASHED;
        if (lower.contains("dribble")) return WAVY;
        return SOLID;
    }
}