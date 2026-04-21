package com.community.athenixback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.storage.path:/tmp/athenix-uploads}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 썸네일 이미지 경로 매핑
        registry.addResourceHandler("/thumbnails/**")
            .addResourceLocations("file:" + storagePath + "/thumbnails/")
            .setCachePeriod(3600); // 1시간 캐시

        // 업로드된 파일 경로 매핑
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + storagePath + "/")
            .setCachePeriod(86400); // 24시간 캐시
    }
}
