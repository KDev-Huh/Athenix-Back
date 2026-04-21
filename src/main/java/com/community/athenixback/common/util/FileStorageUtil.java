 package com.community.athenixback.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileStorageUtil {

    @Value("${app.storage.path:/tmp/athenix-uploads}")
    private String storagePath;

    @Value("${app.storage.max-file-size:5242880}")
    private long maxFileSize;

    private static final String[] ALLOWED_VIDEO_TYPES = {"video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo"};

    public String saveVideoFile(MultipartFile file) throws IOException {
        // 파일 크기 확인
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("파일 크기가 최대 허용 크기를 초과했습니다.");
        }

        // MIME 타입 확인
        String contentType = file.getContentType();
        boolean isValidType = false;
        for (String type : ALLOWED_VIDEO_TYPES) {
            if (type.equals(contentType)) {
                isValidType = true;
                break;
            }
        }
        if (!isValidType) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }

        // 저장 디렉토리 생성
        File dir = new File(storagePath);
        if (!dir.exists()) {
            boolean dirCreated = dir.mkdirs();
            if (!dirCreated) {
                throw new IOException("저장 디렉토리를 생성할 수 없습니다: " + storagePath);
            }
        }

        // 파일명 생성 (UUID + 확장자)
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".mp4";
        String savedFilename = UUID.randomUUID() + extension;
        String filePath = storagePath + File.separator + savedFilename;

        // 파일 저장
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            throw new IOException("파일 저장에 실패했습니다: " + e.getMessage(), e);
        }

        return filePath;
    }

    public void deleteVideoFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // 파일 삭제 실패시 무시
        }
    }

    public long getFileSize(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return 0;
        }
        return Files.size(Paths.get(filePath));
    }
}
