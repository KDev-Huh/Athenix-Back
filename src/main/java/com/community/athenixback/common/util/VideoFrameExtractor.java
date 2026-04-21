package com.community.athenixback.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Slf4j
@Component
public class VideoFrameExtractor {

    /**
     * 동영상에서 특정 시간의 프레임을 JPG로 추출
     *
     * @param videoPath 동영상 파일 경로
     * @param timeMs 추출할 시간 (밀리초)
     * @return 추출된 이미지 바이트 배열
     */
    public byte[] extractFrameAtTime(String videoPath, Long timeMs) {
        try {
            // 시간 변환: milliseconds -> HH:MM:SS.fff
            long totalSeconds = timeMs / 1000;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            long millis = timeMs % 1000;

            String timeString = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);

            log.info("동영상에서 프레임 추출: videoPath={}, timeMs={}, timeString={}",
                videoPath, timeMs, timeString);

            // FFmpeg 명령 실행
            String tempOutputPath = System.getProperty("java.io.tmpdir") + File.separator +
                                   "frame_" + System.currentTimeMillis() + ".jpg";

            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-ss", timeString,
                "-i", videoPath,
                "-vframes", "1",
                "-f", "image2",
                tempOutputPath
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // FFmpeg 출력 로깅
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("FFmpeg 프레임 추출 실패: exitCode={}", exitCode);
                throw new RuntimeException("FFmpeg 프레임 추출 실패");
            }

            // 추출된 이미지 파일을 바이트 배열로 변환
            File imageFile = new File(tempOutputPath);
            if (!imageFile.exists()) {
                throw new RuntimeException("추출된 이미지 파일을 찾을 수 없습니다: " + tempOutputPath);
            }

            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());

            // 임시 파일 삭제
            if (imageFile.delete()) {
                log.debug("임시 이미지 파일 삭제 완료: {}", tempOutputPath);
            }

            log.info("프레임 추출 완료: {} bytes", imageBytes.length);

            return imageBytes;

        } catch (Exception e) {
            log.error("프레임 추출 중 오류 발생", e);
            throw new RuntimeException("프레임 추출 실패: " + e.getMessage(), e);
        }
    }
}
