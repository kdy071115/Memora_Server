# Swagger 테스트 가이드

> Memora 백엔드(Spring Boot) 와 AI 서버(FastAPI) 의 Swagger UI 를 사용해
> API 를 손으로 직접 테스트하는 방법을 정리한 문서입니다.

---

## 0. 사전 준비 - 서버 띄우기

가장 빠른 방법 (Docker 없이):

```bash
cd Memora_Server
./gradlew bootRun --args='--spring.profiles.active=local'
```

부팅 완료 후 브라우저에서 열기:

> 👉 **http://localhost:8080/swagger-ui/index.html**

`local` 프로파일은 H2 인메모리 DB 라서 PostgreSQL 없이도 즉시 동작합니다.

---

## 1. 화면 구조

```
┌──────────────────────────────────────────────────┐
│  Memora API           v1.0.0    [Authorize 🔓]  │ ← 우측 상단 자물쇠
│  Memora AI Learning Copilot 백엔드                │
├──────────────────────────────────────────────────┤
│  Servers: ▼ http://localhost:8080 (Local)        │
│                                                  │
│  ▼ Auth                                          │
│     POST /api/auth/signup   회원가입               │
│     POST /api/auth/login    로그인                 │
│     POST /api/auth/refresh  토큰 재발급             │
│     GET  /api/auth/me       내 정보                │
│                                                  │
│  ▶ Course                                        │
│  ▶ Lecture                                       │
│  ▶ Document                                      │
│  ▶ QA                                            │
│  ▶ Quiz                                          │
│  ▶ Analysis                                      │
└──────────────────────────────────────────────────┘
```

태그(Auth, Course...)를 클릭하면 펼쳐집니다. (`doc-expansion: none` 이라 기본 접힘)

---

## 2. 최소 체험 시나리오 - "회원가입 → 로그인 → 인증 호출"

### 2-1. 회원가입

1. **`Auth`** 태그 클릭 → **`POST /api/auth/signup`** 클릭
2. 우측 **`Try it out`** 버튼 클릭
3. **Request body** 영역에 입력:

   ```json
   {
     "email": "instructor@test.com",
     "password": "Test1234!",
     "name": "테스트교수",
     "role": "INSTRUCTOR"
   }
   ```

4. **`Execute`** 버튼 → 아래 **Response body** 에서 200 응답 확인

> 💡 학생 계정도 만들고 싶으면 `role` 을 `STUDENT` 로 한 번 더 실행

### 2-2. 로그인 (토큰 받기)

1. **`POST /api/auth/login`** → `Try it out`
2. Body:

   ```json
   {
     "email": "instructor@test.com",
     "password": "Test1234!"
   }
   ```

3. `Execute` → 응답에서 **`accessToken`** 값을 통째로 복사 (`eyJhbGciOi...` 로 시작하는 긴 문자열)

응답 예시:

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",   // ← 이거 복사
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "id": 1, "email": "...", "role": "INSTRUCTOR" }
  }
}
```

### 2-3. 🔑 Authorize - 모든 요청에 토큰 자동 부착

1. 페이지 우측 상단 **`Authorize 🔓`** 버튼 클릭
2. 모달 창의 **`Value`** 입력칸에 **방금 복사한 accessToken 값만** 붙여넣기
   - ⚠️ `Bearer ` 접두어를 직접 붙이지 않아도 됨 (자동 추가됨)
3. **`Authorize`** → **`Close`**
4. 자물쇠가 🔒 잠금 상태로 바뀌면 OK

이후 **모든 엔드포인트가 자동으로 `Authorization: Bearer ...` 헤더와 함께 호출**됩니다.

> 💡 `persist-authorization: true` 설정이 켜져 있어 페이지를 새로고침해도 토큰이 유지됩니다.

### 2-4. 인증 확인

1. **`GET /api/auth/me`** → `Try it out` → `Execute`
2. 200 응답으로 본인 정보가 나오면 정상
3. 401 이 나오면 → 토큰을 다시 Authorize 에 등록

---

## 3. 풀 시나리오 - "강의 → 자료 → QA → 퀴즈"

INSTRUCTOR 토큰으로 진행.

### Step 1. 강의 생성

`POST /api/courses`

```json
{
  "title": "운영체제 입문",
  "description": "프로세스/메모리/파일시스템"
}
```

응답에서 `data.id` 기억 (예: `1`)

### Step 2. 차시 생성

`POST /api/courses/1/lectures`

```json
{
  "title": "1주차 - 프로세스 관리",
  "week": 1
}
```

응답에서 `data.id` 기억 (예: `1`)

### Step 3. PDF 자료 업로드

`POST /api/lectures/1/documents`

이건 multipart 라서 입력 방식이 다릅니다:

1. `Try it out`
2. **`file`** 파라미터의 **`Choose File`** 버튼으로 PDF 선택
3. `Execute`

> ⚠️ AI 서버가 떠 있어야 임베딩이 진행됩니다 (`uvicorn main:app --port 8000`).
> AI 서버 없이 업로드만 테스트하려면 S3 키 미설정 시 S3 단계에서 에러가 날 수 있습니다.

### Step 4. QA 세션 생성 + 질문

```text
POST /api/lectures/1/qa/sessions
→ data.id 복사 (예: 1)

POST /api/qa/sessions/1/messages
{
  "content": "프로세스와 스레드의 차이를 설명해줘",
  "difficulty": "MEDIUM"
}
```

응답에 **`sources`** 가 있으면 RAG 검색이 동작한 것.

### Step 5. 퀴즈 생성 + 풀이

```text
POST /api/lectures/1/quizzes/generate
{ "count": 5, "type": "MULTIPLE_CHOICE", "difficulty": "MEDIUM" }

GET /api/lectures/1/quizzes
→ 생성된 퀴즈 목록 확인, 첫 quiz id 복사

POST /api/quizzes/{quizId}/submit
{ "answer": "1" }
→ isCorrect / feedback 확인
```

### Step 6. 학습 분석

```text
GET /api/analysis/me
→ weakConcepts / weeklyProgress / 진단 / 추천
```

---

## 4. 자주 쓰는 기능

| 기능 | 위치 | 설명 |
|---|---|---|
| **요청 시간 확인** | Response 우측 | `display-request-duration` 옵션 켜둠 → ms 표시 |
| **cURL 복사** | Response → Curl 영역 | 동일 요청을 터미널에서 재현 가능 |
| **스키마 보기** | 각 엔드포인트 우측 `Schema` 탭 | DTO 필드/타입 확인 |
| **Try it out 취소** | `Cancel` | 다시 documentation 모드 |
| **그룹 펼침/접기** | 태그 헤더 클릭 | doc-expansion=none 이라 기본 접힘 |
| **검색 필터** | 상단 검색창 | 엔드포인트 빠르게 찾기 |
| **Authorize 해제** | 자물쇠 → Logout | 토큰 제거 후 401 동작 검증 |

---

## 5. 문제 해결

| 증상 | 원인 / 해결 |
|---|---|
| **401 Unauthorized** | Authorize 안 했거나 토큰 만료(1h). 로그인 다시 → Authorize 다시 |
| **403 Forbidden** | 권한 부족 (예: 학생 계정으로 강의 생성 시도). INSTRUCTOR 로 로그인 |
| **400 Bad Request** | 요청 body 검증 실패. 응답 본문에 어떤 필드가 문제인지 표시됨 |
| **500 + AI_SERVER_ERROR** | AI 서버 미기동. `uvicorn main:app --port 8000` 띄우기 |
| **Document 업로드 시 S3 에러** | `.env` 의 AWS 키 미설정. S3 미사용이면 업로드 스킵 |
| **CORS 에러는 안 남** | Swagger UI 가 같은 호스트라 CORS 무관 |
| **`Failed to fetch`** | 백엔드 미기동 / 다른 포트. 콘솔 로그 확인 |

---

## 6. FastAPI(AI 서버) Swagger 도 체험

별도 터미널에서 AI 서버를 띄우고:

```bash
cd memora-ai
source venv/bin/activate         # 최초 1회: python3 -m venv venv && pip install -r requirements.txt
uvicorn main:app --reload
```

→ **http://localhost:8000/docs** (Swagger UI)
→ **http://localhost:8000/redoc** (ReDoc)

FastAPI Swagger 에서 할 수 있는 것:

1. **`/ai/health`** → `Try it out` → `Execute` 로 헬스체크
2. **`/ai/qa/ask`** 등 RAG 엔드포인트를 백엔드 거치지 않고 직접 호출
3. 응답 스키마 확인 → 백엔드의 `infra/ai/dto/*` 와 일치 여부 검증

> 💡 FastAPI 쪽은 기본 인증 없음. 운영 시 reverse proxy / 토큰 추가 권장.

---

## 7. 시연 직전 체크리스트

- [ ] `./gradlew bootRun --args='--spring.profiles.active=local'` 또는 `--prod` 로 백엔드 기동
- [ ] (필요시) `uvicorn main:app --port 8000` 으로 AI 서버 기동
- [ ] http://localhost:8080/swagger-ui/index.html 접속
- [ ] INSTRUCTOR 회원가입
- [ ] STUDENT 회원가입 (선택)
- [ ] 로그인 → accessToken 복사
- [ ] 우측 상단 Authorize 🔓 → 토큰 등록
- [ ] `GET /api/auth/me` 200 확인
- [ ] 강의/차시/자료/QA/Quiz/Analysis 시연 흐름 1회 리허설

---

## 8. 한 줄 요약

```text
1. 서버 띄우기  → ./gradlew bootRun --args='--spring.profiles.active=local'
2. 회원가입     → POST /api/auth/signup
3. 로그인       → POST /api/auth/login  (accessToken 복사)
4. Authorize 🔒 → 우측 상단 자물쇠 클릭 → 토큰 붙여넣기
5. 나머지 모든 API 자유롭게 테스트
```
