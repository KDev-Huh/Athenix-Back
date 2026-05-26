package com.community.athenixback.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = new File(System.getProperty("user.dir"), ".env");
        if (!envFile.exists()) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try {
            for (String line : Files.readAllLines(envFile.toPath())) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int idx = line.indexOf('=');
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                // 실제 OS 환경변수가 있으면 우선하도록 .env는 낮은 우선순위로 추가
                if (!environment.containsProperty(key)) {
                    props.put(key, value);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(".env 파일 읽기 실패: " + envFile.getAbsolutePath(), e);
        }

        environment.getPropertySources().addLast(new MapPropertySource("dotenv", props));
    }
}
