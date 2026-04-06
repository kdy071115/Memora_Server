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

세 가지 모드 중 상황에 맞게 선택:

| 모드 | DB | 데이터 영속성 | 권장 용도 |
|------|----|---|---|
| **A. local 프로파일 (H2)** | H2 인메모리 | ❌ 재시작 시 초기화 | 빠른 로직 확인, Docker 불가 환경 |
| **B. dev 워크플로 (PostgreSQL)** | PostgreSQL (Docker) | ✅ 볼륨 영속화 | **권장 - 안정적 개발** |
| **C. 통합 실행 (Docker Compose)** | PostgreSQL (Docker) | ✅ 볼륨 영속화 | 백엔드 + AI 서버 통합 테스트 |

### A) Spring Boot 단독 실행 (H2 인메모리, Docker 불필요)

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 콘솔: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:memora;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`
  - User: `sa`, Password: (빈 값)

### B) PostgreSQL + Spring Boot 호스트 실행 (권장)

가장 안정적인 개발 환경. PostgreSQL만 Docker로 띄우고, 백엔드는 호스트에서 실행해서 핫리로드/디버깅이 자유롭습니다.

```bash
# 1. 환경 변수 파일 생성 (최초 한 번)
cp .env.example .env

# 2. PostgreSQL 컨테이너만 띄우기 (Memora 루트에서)
cd ..
docker compose -f docker-compose-dev.yml up -d

# 3. Spring Boot 실행 (Memora_Server 디렉터리에서)
cd Memora_Server
./gradlew bootRun
```

`spring-dotenv` 가 적용되어 있어 `Memora_Server/.env` 파일이 자동으로 로드됩니다.

종료 시:

```bash
docker compose -f docker-compose-dev.yml down       # 데이터 유지
docker compose -f docker-compose-dev.yml down -v    # 데이터까지 삭제
```

### C) 통합 실행 (Docker Compose)

루트 디렉터리(`Memora/`)에서 백엔드 + AI 서버 + DB 모두 한 번에:

```bash
cp .env.example .env  # 값 수정 (특히 OPENAI_API_KEY)
docker compose up --build
```

기동 후:
- Backend: http://localhost:8080
- AI Server: http://localhost:8000
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 환경 변수 파일(.env)

`.env` 파일은 git에 커밋되지 않습니다 (`.gitignore` 등록됨).

- `Memora_Server/.env.example` → 백엔드 단독 실행용 (DB, JWT, AI 서버, AWS)
- `Memora/.env.example` → 통합 Docker Compose 실행용 (위 + OpenAI 키 등)

`.env` 의 값은 `application.yml` 의 환경 변수 fallback 으로 주입됩니다. 즉 `.env` 파일이 없거나 값이 비어 있어도 코드 상의 기본값으로 동작합니다.

## 주요 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `SPRING_DATASOURCE_URL` | PostgreSQL 접속 URL | `jdbc:postgresql://localhost:5432/memora` |
| `DB_USERNAME` / `DB_PASSWORD` | DB 계정 | `memora` / `memora` |
| `JWT_SECRET` | JWT 서명 키 (운영 환경에서는 반드시 교체) | (개발용 기본값) |
| `AI_SERVER_URL` | FastAPI 서버 URL | `http://localhost:8000` |
| `AWS_S3_BUCKET` | S3 버킷 | `memora-uploads` |
| `AWS_REGION` | AWS 리전 | `ap-northeast-2` |
| `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` | AWS 자격 증명 | (빈 값 - 미사용 시 비활성화) |
| `OPENAI_API_KEY` | OpenAI 키 (AI 서버 측, AI 서버 .env 에서 사용) | - |

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
