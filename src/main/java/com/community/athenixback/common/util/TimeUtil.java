package com.community.athenixback.common.util;

public class TimeUtil {

    /**
     * milliseconds를 mm:ss.SSS 형식으로 변환
     */
    public static String msToTimeLabel(Long timeMs) {
        if (timeMs == null || timeMs < 0) {
            return "00:00.000";
        }

        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = timeMs % 1000;

        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}
