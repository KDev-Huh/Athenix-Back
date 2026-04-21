# 7.1 AI 피드백 API 구현 문서

## 📋 개요

사용자가 경기(Match)의 특정 시간(timeMs)을 입력하면, 해당 시점의 영상 프레임을 추출하여 AI 서버로 전송하고, AI 서버의 분석 결과(situation + playGuide)를 받아서 저장 및 반환하는 API입니다.

## 🔄 구현 흐름

```
사용자 요청
   ↓
POST /api/v1/matches/{matchId}/ai-feedback
   ↓
1️⃣ VideoFrameExtractor: timeMs → FFmpeg으로 프레임 추출 (JPG)
   ↓
2️⃣ AIServerClient: 이미지를 AI 서버로 전송 (multipart/form-data)
   ↓
3️⃣ AI 서버 (POST /ai/recommend/image)
   - 이미지 분석
   - situation, playGuide 생성 (LLM 사용)
   ↓
4️⃣ AIFeedbackService: 응답 저장 + 추가 필드 생성
   - feedbackId ← DB auto-increment
   - timeMs ← 사용자 제공
   - timeLabel ← TimeUtil로 mm:ss.SSS 변환
   - situation ← AI 서버에서 받음
   - playGuide ← AI 서버에서 받음
   - createdAt ← @PrePersist로 자동 설정
   ↓
클라이언트에 응답 반환
```

## 📦 생성된 파일 구조

```
src/main/java/com/community/athenixback/
├── match/
│   ├── controller/
│   │   └── AIFeedbackController.java (API 엔드포인트)
│   ├── service/
│   │   └── AIFeedbackService.java (비즈니스 로직)
│   ├── client/
│   │   └── AIServerClient.java (AI 서버 통신)
│   ├── dto/
│   │   ├── AIFeedbackRequest.java (요청 DTO)
│   │   ├── AIFeedbackResponse.java (응답 DTO)
│   │   ├── AIServerResponse.java (AI 서버 응답 DTO)
│   │   └── PlayGuideDto.java (playGuide 객체)
│   ├── entity/
│   │   └── AIFeedback.java (데이터베이스 엔티티)
│   └── repository/
│       └── AIFeedbackRepository.java (JPA Repository)
├── common/
│   ├── util/
│   │   ├── VideoFrameExtractor.java (FFmpeg 프레임 추출)
│   │   └── TimeUtil.java (시간 변환)
│   └── response/
│       └── ApiResponse.java (표준 응답 형식)
└── config/
    └── RestTemplateConfig.java (RestTemplate + ObjectMapper Bean)
```

## 🔌 API 엔드포인트

### 7.1 AI 피드백 생성
```
POST /api/v1/matches/{matchId}/ai-feedback
Content-Type: application/json

Request:
{
  "timeMs": 754321
}

Response:
{
  "success": true,
  "data": {
    "feedbackId": 1,
    "matchId": 10,
    "timeMs": 754321,
    "timeLabel": "12:34.321",
    "situation": "수비 라인이 좁아졌고 공격팀 캐리어가 상대 진영에 위치합니다.",
    "playGuide": {
      "type": "pass",
      "start_x": 42.1,
      "start_y": 58.3,
      "end_x": 71.8,
      "end_y": 44.6,
      "message": "수비를 끌어낸 뒤 오른쪽 하프스페이스로 스루패스를 시도하세요."
    },
    "createdAt": "2026-04-21T09:10:00Z"
  },
  "error": null
}
```

### 7.2 AI 피드백 이력 조회
```
GET /api/v1/matches/{matchId}/ai-feedback?page=1&size=20&sort=latest
```

### 7.3 AI 피드백을 메모로 추가
```
POST /api/v1/matches/{matchId}/ai-feedback/memo
```

## ⚙️ 설정 파일 (application.properties)

```properties
# AI Server 설정
ai.server.url=http://localhost:8000              # ✅ 실제 AI 서버 IP/PORT로 변경
ai.server.endpoint.recommend=/ai/recommend/image
```

## 🔧 수정 방법 (실제 AI 서버 연결 시)

### 1️⃣ AI 서버 IP/PORT 변경
```properties
# application.properties
# 변경 전:
ai.server.url=http://localhost:8000

# 변경 후 (예시):
ai.server.url=http://192.168.1.100:8000
또는
ai.server.url=http://ai-server.example.com:8000
```

### 2️⃣ application-local.properties 설정 (로컬 개발)
```properties
# application-local.properties
ai.server.url=http://localhost:8000
```

### 3️⃣ 환경 변수로 설정 (운영 환경)
```bash
export AI_SERVER_URL=http://production-ai-server.com:8000
```

Spring Boot에서 환경 변수 사용:
```properties
ai.server.url=${AI_SERVER_URL:http://localhost:8000}
```

## 📝 핵심 구현 요소

### VideoFrameExtractor
- **용도**: FFmpeg을 사용하여 동영상의 특정 시간대 프레임 추출
- **입력**: videoPath, timeMs
- **출력**: 이미지 바이트 배열 (JPG)
- **주의**: FFmpeg이 서버에 설치되어 있어야 함

```bash
# FFmpeg 설치 (Mac)
brew install ffmpeg

# FFmpeg 설치 (Linux - Ubuntu/Debian)
sudo apt-get install ffmpeg

# FFmpeg 설치 (Windows)
# https://ffmpeg.org/download.html 에서 다운로드
```

### AIServerClient
- **용도**: AI 서버와의 HTTP 통신
- **메서드**: `requestPlayRecommendation(byte[] imageBytes, String fileName)`
- **전송 형식**: multipart/form-data (이미지 파일)
- **응답 형식**: JSON (situation + playGuide)

### TimeUtil
- **메서드**: `msToTimeLabel(Long timeMs)`
- **변환**: milliseconds → mm:ss.SSS 형식
- **예시**: 754321ms → "12:34.321"

## 🗄️ 데이터베이스 스키마

```sql
CREATE TABLE ai_feedbacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    time_ms BIGINT NOT NULL,
    situation TEXT NOT NULL,
    play_guide_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    INDEX idx_match_id (match_id),
    INDEX idx_created_at (created_at)
);
```

## 🎯 AI 서버 명세 요약

### 요청
```
POST /ai/recommend/image
Content-Type: multipart/form-data

Parameters:
- image: 분석할 경기 장면 이미지 (jpg, png, webp)
```

### 응답
```json
{
  "success": true,
  "data": {
    "situation": "string (LLM 분석)",
    "playGuide": {
      "type": "pass|shot|dribble|..." (StatsBomb 이벤트 타입),
      "start_x": 0~120 (필수),
      "start_y": 0~80 (필수),
      "end_x": 0~120 또는 null (선택),
      "end_y": 0~80 또는 null (선택),
      "message": "string (LLM 생성)"
    }
  },
  "error": null
}
```

### 에러 응답
- `400`: 지원하지 않는 이미지 형식
- `404`: 유사 장면을 찾지 못함
- `422`: 호모그래피 계산 실패 / 선수 미감지
- `500`: LLM 서버(Ollama) 연결 실패

## 🚀 배포 시 체크리스트

- [ ] FFmpeg이 서버에 설치되어 있는지 확인
- [ ] AI 서버 IP/PORT가 올바른지 확인
- [ ] AI 서버가 정상 작동하는지 테스트
- [ ] 임시 이미지 저장 디렉토리 권한 확인 (`/tmp`)
- [ ] 네트워크 방화벽에서 AI 서버 포트가 열려있는지 확인

## 💡 추가 개선 사항 (선택사항)

1. **이미지 캐싱**: 동일한 timeMs의 이미지를 캐싱하여 FFmpeg 실행 횟수 감소
2. **비동기 처리**: AI 분석을 비동기로 처리하여 응답 시간 단축
3. **에러 재시도**: AI 서버 연결 실패 시 재시도 로직 추가
4. **이미지 압축**: 업로드 전 이미지 압축으로 네트워크 대역폭 절감
