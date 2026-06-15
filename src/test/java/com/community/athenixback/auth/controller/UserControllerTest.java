package com.community.athenixback.auth.controller;

import com.community.athenixback.auth.dto.LoginRequest;
import com.community.athenixback.auth.dto.SignupRequest;
import com.community.athenixback.auth.entity.User;
import com.community.athenixback.auth.repository.UserRepository;
import com.community.athenixback.match.entity.Match;
import com.community.athenixback.match.entity.Memo;
import com.community.athenixback.match.repository.MatchRepository;
import com.community.athenixback.match.repository.MemoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:user-controller-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "jwt.secret=test-secret-key-test-secret-key-test-secret-key-test-secret-key-1234"
})
class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MemoRepository memoRepository;

    @Test
    void getMySummaryReturnsAuthenticatedUserSummary() throws Exception {
        SignupRequest signupRequest = SignupRequest.builder()
            .name("홍길동")
            .position("FW")
            .email("summary@example.com")
            .password("password1234")
            .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> signupResponse = client.send(jsonPost("/api/v1/auth/signup", signupRequest),
            HttpResponse.BodyHandlers.ofString());
        assertThat(signupResponse.statusCode())
            .as(signupResponse.body())
            .isEqualTo(201);

        LoginRequest loginRequest = LoginRequest.builder()
            .email(signupRequest.getEmail())
            .password(signupRequest.getPassword())
            .build();

        HttpResponse<String> loginResponse = client.send(jsonPost("/api/v1/auth/login", loginRequest),
            HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode())
            .as(loginResponse.body())
            .isEqualTo(200);

        User user = userRepository.findByEmail(signupRequest.getEmail()).orElseThrow();
        Match firstMatch = matchRepository.save(Match.builder()
            .user(user)
            .title("첫 번째 경기")
            .date("2026-06-15")
            .description("테스트 경기")
            .status("분석 완료")
            .build());
        Match secondMatch = matchRepository.save(Match.builder()
            .user(user)
            .title("두 번째 경기")
            .date("2026-06-16")
            .description("테스트 경기")
            .status("임시 저장")
            .build());
        memoRepository.save(Memo.builder()
            .match(firstMatch)
            .timeMs(1000L)
            .text("일반 메모")
            .label("일반")
            .build());
        memoRepository.save(Memo.builder()
            .match(firstMatch)
            .timeMs(2000L)
            .text("AI 피드백 메모")
            .label("AI 피드백")
            .build());
        memoRepository.save(Memo.builder()
            .match(secondMatch)
            .timeMs(3000L)
            .text("AI 피드백 메모")
            .label("AI 피드백")
            .build());

        JsonNode loginJson = objectMapper.readTree(loginResponse.body());
        String accessToken = loginJson.path("data").path("accessToken").asText();

        HttpRequest summaryRequest = HttpRequest.newBuilder(uri("/api/v1/users/me/summary"))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        HttpResponse<String> summaryResponse = client.send(summaryRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(summaryResponse.statusCode())
            .as(summaryResponse.body())
            .isEqualTo(200);

        JsonNode summaryJson = objectMapper.readTree(summaryResponse.body());
        assertThat(summaryJson.path("success").asBoolean()).isTrue();
        assertThat(summaryJson.path("data").path("totalMatches").asInt()).isEqualTo(2);
        assertThat(summaryJson.path("data").path("totalMemos").asInt()).isEqualTo(3);
        assertThat(summaryJson.path("data").path("aiAnalysisCount").asInt()).isEqualTo(2);
    }

    @Test
    void getMySummaryReturnsZeroCountsWhenUserHasNoData() throws Exception {
        SignupRequest signupRequest = SignupRequest.builder()
            .name("김영희")
            .position("MF")
            .email("empty-summary@example.com")
            .password("password1234")
            .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> signupResponse = client.send(jsonPost("/api/v1/auth/signup", signupRequest),
            HttpResponse.BodyHandlers.ofString());
        assertThat(signupResponse.statusCode())
            .as(signupResponse.body())
            .isEqualTo(201);

        LoginRequest loginRequest = LoginRequest.builder()
            .email(signupRequest.getEmail())
            .password(signupRequest.getPassword())
            .build();

        HttpResponse<String> loginResponse = client.send(jsonPost("/api/v1/auth/login", loginRequest),
            HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode())
            .as(loginResponse.body())
            .isEqualTo(200);

        JsonNode loginJson = objectMapper.readTree(loginResponse.body());
        String accessToken = loginJson.path("data").path("accessToken").asText();

        HttpRequest summaryRequest = HttpRequest.newBuilder(uri("/api/v1/users/me/summary"))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        HttpResponse<String> summaryResponse = client.send(summaryRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(summaryResponse.statusCode())
            .as(summaryResponse.body())
            .isEqualTo(200);

        JsonNode summaryJson = objectMapper.readTree(summaryResponse.body());
        assertThat(summaryJson.path("success").asBoolean()).isTrue();
        assertThat(summaryJson.path("data").path("totalMatches").asInt()).isZero();
        assertThat(summaryJson.path("data").path("totalMemos").asInt()).isZero();
        assertThat(summaryJson.path("data").path("aiAnalysisCount").asInt()).isZero();
    }

    @Test
    void getMySummaryReturnsUnauthorizedWithoutToken() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest summaryRequest = HttpRequest.newBuilder(uri("/api/v1/users/me/summary"))
            .GET()
            .build();

        HttpResponse<String> summaryResponse = client.send(summaryRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(summaryResponse.statusCode())
            .as(summaryResponse.body())
            .isEqualTo(401);

        JsonNode summaryJson = objectMapper.readTree(summaryResponse.body());
        assertThat(summaryJson.path("success").asBoolean()).isFalse();
        assertThat(summaryJson.path("error").path("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    private HttpRequest jsonPost(String path, Object body) throws Exception {
        return HttpRequest.newBuilder(uri(path))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
