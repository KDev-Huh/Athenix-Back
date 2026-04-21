package com.community.athenixback.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

@Slf4j
@Component
public class VideoThumbnailUtil {

    @Value("${app.storage.path:/tmp/athenix-uploads}")
    private String storagePath;

    private static final String THUMBNAIL_DIR = "thumbnails";

    /**
     * 영상 파일에서 특정 시간의 썸네일 추출
     * @param videoFilePath 영상 파일 경로
     * @param timeMs 시간 (밀리초)
     * @return 썸네일 파일 상대 경로
     */
    public String generateThumbnail(String videoFilePath, long timeMs) {
        try {
            // 썸네일 디렉토리 생성
            File thumbDir = new File(storagePath, THUMBNAIL_DIR);
            if (!thumbDir.exists()) {
                thumbDir.mkdirs();
            }

            // 썸네일 파일명 생성
            String thumbnailFilename = UUID.randomUUID() + ".jpg";
            String thumbnailPath = new File(thumbDir, thumbnailFilename).getAbsolutePath();

            // 시간을 HH:mm:ss 형식으로 변환
            long totalSeconds = timeMs / 1000;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            String timeFormat = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            // ffmpeg 명령어 실행
            String[] command = {
                "ffmpeg",
                "-i", videoFilePath,
                "-ss", timeFormat,
                "-vframes", "1",
                "-vf", "scale=320:240",
                "-y",
                thumbnailPath
            };

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 프로세스 출력 읽기 (timeout 방지)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("ffmpeg: {}", line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && new File(thumbnailPath).exists()) {
                log.info("썸네일 생성 완료: {}", thumbnailPath);
                // 상대 경로 반환
                return "/thumbnails/" + thumbnailFilename;
            } else {
                log.error("ffmpeg 실행 실패: exitCode={}", exitCode);
                return null;
            }

        } catch (Exception e) {
            log.error("썸네일 생성 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 썸네일 파일 삭제
     */
    public void deleteThumbnail(String thumbnailUrl) {
        try {
            if (thumbnailUrl != null && thumbnailUrl.startsWith("/thumbnails/")) {
                String filename = thumbnailUrl.substring("/thumbnails/".length());
                File file = new File(storagePath, THUMBNAIL_DIR + File.separator + filename);
                if (file.exists()) {
                    file.delete();
                    log.info("썸네일 삭제 완료: {}", file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("썸네일 삭제 중 오류: {}", e.getMessage());
        }
    }
}
