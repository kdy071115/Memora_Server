# Memora Server

AI 기반 학습 코파일럿 **Memora** 의 백엔드 (Spring Boot).

## 구성

| 구성요소 | 기술 | 포트 |
|---------|------|------|
| Backend | Spring Boot 3.5 / Java 17 | 8080 |
| AI Server | FastAPI + LangChain | 8000 |
| DB | PostgreSQL 16 | 5432 |
| Vector Store | FAISS (per-lecture) | - |
| Storage | AWS S3 | - |

## 로컬 실행

### 1) Spring Boot 단독 실행

```bash
./gradlew bootRun
```

`http://localhost:8080/swagger-ui/index.html` 에서 API 문서 확인.

### 2) 통합 실행 (Docker Compose)

루트 디렉터리(`Memora/`)에서:

```bash
cp .env.example .env  # 값 수정
docker compose up --build
```

기동 후:
- Backend: http://localhost:8080
- AI Server: http://localhost:8000
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 주요 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `SPRING_DATASOURCE_URL` | PostgreSQL 접속 URL | `jdbc:postgresql://localhost:5432/memora` |
| `DB_USERNAME` / `DB_PASSWORD` | DB 계정 | `memora` / `memora` |
| `JWT_SECRET` | JWT 서명 키 | (개발용 기본값) |
| `AI_SERVER_URL` | FastAPI 서버 URL | `http://localhost:8000` |
| `AWS_S3_BUCKET` | S3 버킷 | `memora-uploads` |
| `OPENAI_API_KEY` | OpenAI 키 (AI 서버) | - |

## 디렉터리 구조

```
src/main/java/com/kit/memora_server
├── domain/                # 도메인별 (auth, user, course, lecture, document, qa, quiz, analysis)
│   └── {domain}/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       └── dto/
├── global/                # 공통 (common, config, security, exception)
└── infra/                 # 외부 연동 (ai)
```

## API 카테고리

- 인증 (`/api/auth/*`)
- 강의/차시/자료 (`/api/courses`, `/api/lectures`, `/api/documents`)
- AI 질의응답 (`/api/qa/*`)
- 퀴즈 생성/풀이 (`/api/quizzes/*`)
- 학습 분석 (`/api/analysis/*`)

자세한 명세는 `docs/FRONTEND_HANDOFF.md` 또는 Swagger UI 참고.
