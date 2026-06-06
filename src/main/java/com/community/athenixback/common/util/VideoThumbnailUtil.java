package com.community.athenixback.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

@Slf4j
@Component
public class VideoThumbnailUtil {

    @Value("${app.storage.path:/tmp/athenix-uploads}")
    private String storagePath;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    private static final String THUMBNAIL_DIR = "thumbnails";

    public static class ProcessedVideo {
        public final String videoPath;
        public final String thumbnailUrl;
        public final long durationSec;

        public ProcessedVideo(String videoPath, String thumbnailUrl, long durationSec) {
            this.videoPath = videoPath;
            this.thumbnailUrl = thumbnailUrl;
            this.durationSec = durationSec;
        }
    }

    /**
     * 트랜스코딩 + 썸네일 추출을 ffmpeg 한 번에 처리.
     * 이미 H.264+AAC면 stream copy (수 초), 아니면 ultrafast 재인코딩.
     */
    public ProcessedVideo processVideoFile(String inputPath) {
        String outputPath = inputPath.replaceAll("\\.[^.]+$", "") + "_h264.mp4";

        File thumbDir = new File(storagePath, THUMBNAIL_DIR);
        if (!thumbDir.exists()) thumbDir.mkdirs();
        String thumbnailFilename = UUID.randomUUID() + ".jpg";
        String thumbnailPath = new File(thumbDir, thumbnailFilename).getAbsolutePath();

        VideoInfo info = probeVideo(inputPath);
        log.info("ffprobe 결과: video={}, audio={}, duration={}s", info.videoCodec, info.audioCodec, info.durationSec);

        boolean streamCopy = "h264".equals(info.videoCodec)
                && ("aac".equals(info.audioCodec) || info.audioCodec.isEmpty());

        // 트랜스코딩 + 썸네일을 ffmpeg 한 번에
        String[] command = buildFfmpegCommand(inputPath, outputPath, thumbnailPath, streamCopy);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (reader.readLine() != null) { /* drain */ }

            int exitCode = process.waitFor();
            if (exitCode == 0 && new File(outputPath).exists()) {
                new File(inputPath).delete();
                log.info("영상 처리 완료 ({}): {}", streamCopy ? "stream copy" : "재인코딩", outputPath);

                String thumbnailUrl = new File(thumbnailPath).exists()
                        ? "/thumbnails/" + thumbnailFilename : null;
                return new ProcessedVideo(outputPath, thumbnailUrl, info.durationSec);
            } else {
                log.error("ffmpeg 실패: exitCode={}", exitCode);
                return new ProcessedVideo(inputPath, null, info.durationSec);
            }
        } catch (Exception e) {
            log.error("영상 처리 중 오류: {}", e.getMessage(), e);
            return new ProcessedVideo(inputPath, null, info.durationSec);
        }
    }

    // 메모 생성 시 특정 시간 프레임 추출용
    public String generateThumbnail(String videoFilePath, long timeMs) {
        try {
            File thumbDir = new File(storagePath, THUMBNAIL_DIR);
            if (!thumbDir.exists()) thumbDir.mkdirs();

            String thumbnailFilename = UUID.randomUUID() + ".jpg";
            String thumbnailPath = new File(thumbDir, thumbnailFilename).getAbsolutePath();

            long totalSeconds = timeMs / 1000;
            String timeFormat = String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);

            String[] command = {
                ffmpegPath, "-y", "-i", videoFilePath,
                "-ss", timeFormat, "-vframes", "1",
                "-vf", "scale=320:240",
                thumbnailPath
            };

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (reader.readLine() != null) { /* drain */ }

            int exitCode = process.waitFor();
            if (exitCode == 0 && new File(thumbnailPath).exists()) {
                return "/thumbnails/" + thumbnailFilename;
            }
            log.error("썸네일 생성 실패: exitCode={}", exitCode);
            return null;
        } catch (Exception e) {
            log.error("썸네일 생성 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }

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

    // ffprobe 한 번으로 코덱 + duration 동시 추출
    private VideoInfo probeVideo(String inputPath) {
        String ffprobePath = ffmpegPath.replace("ffmpeg", "ffprobe");
        try {
            Process probe = new ProcessBuilder(
                ffprobePath, "-v", "error",
                "-show_entries", "stream=codec_name,codec_type:format=duration",
                "-of", "default=noprint_wrappers=1",
                inputPath
            ).start();

            String videoCodec = "";
            String audioCodec = "";
            long durationSec = 0;

            BufferedReader reader = new BufferedReader(new InputStreamReader(probe.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("codec_name=")) {
                    // codec_type이 다음 줄에 오므로 codec_type 기준으로 분류
                    String codec = line.substring("codec_name=".length()).trim();
                    String nextLine = reader.readLine();
                    if (nextLine != null && nextLine.startsWith("codec_type=")) {
                        String type = nextLine.substring("codec_type=".length()).trim();
                        if ("video".equals(type)) videoCodec = codec;
                        else if ("audio".equals(type)) audioCodec = codec;
                    }
                } else if (line.startsWith("duration=")) {
                    String val = line.substring("duration=".length()).trim();
                    try {
                        durationSec = (long) Double.parseDouble(val);
                    } catch (NumberFormatException ignored) {}
                }
            }
            probe.waitFor();
            return new VideoInfo(videoCodec, audioCodec, durationSec);
        } catch (Exception e) {
            log.warn("ffprobe 실패, 재인코딩으로 fallback: {}", e.getMessage());
            return new VideoInfo("", "", 0);
        }
    }

    private String[] buildFfmpegCommand(String input, String output, String thumbnail, boolean streamCopy) {
        // ffmpeg 한 번으로 트랜스코딩 + 썸네일 동시 추출
        if (streamCopy) {
            return new String[]{
                ffmpegPath, "-y", "-i", input,
                "-c", "copy", "-movflags", "+faststart", output,
                "-vf", "select=eq(n\\,0),scale=320:240", "-frames:v", "1", thumbnail
            };
        } else {
            return new String[]{
                ffmpegPath, "-y", "-i", input,
                "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", "-movflags", "+faststart", output,
                "-vf", "select=eq(n\\,0),scale=320:240", "-frames:v", "1", thumbnail
            };
        }
    }

    private static class VideoInfo {
        final String videoCodec;
        final String audioCodec;
        final long durationSec;

        VideoInfo(String videoCodec, String audioCodec, long durationSec) {
            this.videoCodec = videoCodec;
            this.audioCodec = audioCodec;
            this.durationSec = durationSec;
        }
    }
}