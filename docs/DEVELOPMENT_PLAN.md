# Memora 개발 계획서 (Backend + AI Server)

> 최종 수정: 2026-04-06
> 상태: 개발 전 설계 단계

---

## 목차

1. [전체 아키텍처](#1-전체-아키텍처)
2. [개발 단계 요약](#2-개발-단계-요약)
3. [STEP 1: 프로젝트 기반 세팅](#step-1-프로젝트-기반-세팅)
4. [STEP 2: DB 설계 및 엔티티 구현](#step-2-db-설계-및-엔티티-구현)
5. [STEP 3: 인증/인가 시스템](#step-3-인증인가-시스템)
6. [STEP 4: 강의 및 자료 관리 API](#step-4-강의-및-자료-관리-api)
7. [STEP 5: FastAPI AI 서버 구축](#step-5-fastapi-ai-서버-구축)
8. [STEP 6: RAG 파이프라인 구현](#step-6-rag-파이프라인-구현)
9. [STEP 7: AI 질의응답 API](#step-7-ai-질의응답-api)
10. [STEP 8: 문제 생성 및 채점 시스템](#step-8-문제-생성-및-채점-시스템)
11. [STEP 9: 학습 분석 시스템](#step-9-학습-분석-시스템)
12. [STEP 10: 통합 테스트 및 배포](#step-10-통합-테스트-및-배포)
13. [DB 스키마 설계](#db-스키마-설계)
14. [API 명세 요약](#api-명세-요약)
15. [프롬프트 설계](#프롬프트-설계)
16. [디렉토리 구조](#디렉토리-구조)

---

## 1. 전체 아키텍처

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   Frontend   │────▶│  Spring Boot API  │────▶│  FastAPI AI Server│
│  (Next.js)   │◀────│   (Port 8080)     │◀────│   (Port 8000)     │
└──────────────┘     └────────┬─────────┘     └────────┬─────────┘
                              │                         │
                     ┌────────▼─────────┐     ┌────────▼─────────┐
                     │   PostgreSQL      │     │   FAISS          │
                     │   (RDS)           │     │   (Vector Store) │
                     └──────────────────┘     └──────────────────┘
                              │
                     ┌────────▼─────────┐
                     │   AWS S3          │
                     │   (파일 저장)      │
                     └──────────────────┘
```

### 통신 흐름

1. **프론트엔드 → Spring Boot**: REST API (JSON)
2. **Spring Boot → FastAPI**: WebClient (비동기 HTTP)
3. **FastAPI → OpenAI**: LangChain을 통한 GPT 호출
4. **FastAPI → FAISS**: 벡터 검색
5. **Spring Boot → PostgreSQL**: JPA
6. **Spring Boot → S3**: AWS SDK (파일 업로드/다운로드)

---

## 2. 개발 단계 요약

| 단계 | 내용 | 예상 기간 | 우선순위 |
|------|------|----------|---------|
| STEP 1 | 프로젝트 기반 세팅 | 1-2일 | P0 |
| STEP 2 | DB 설계 및 엔티티 | 2-3일 | P0 |
| STEP 3 | 인증/인가 | 2-3일 | P0 |
| STEP 4 | 강의/자료 관리 API | 3-4일 | P0 |
| STEP 5 | FastAPI AI 서버 구축 | 2-3일 | P0 |
| STEP 6 | RAG 파이프라인 | 3-4일 | P0 |
| STEP 7 | AI 질의응답 API | 3-4일 | P0 |
| STEP 8 | 문제 생성/채점 | 3-4일 | P1 |
| STEP 9 | 학습 분석 | 2-3일 | P1 |
| STEP 10 | 통합 테스트/배포 | 3-4일 | P0 |

---

## STEP 1: 프로젝트 기반 세팅

### 1.1 Spring Boot 의존성 추가

```gradle
dependencies {
    // Web
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    // JPA + DB
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'

    // Security + JWT
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // Validation
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // AWS S3
    implementation 'software.amazon.awssdk:s3:2.25.0'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8'

    // Configuration Processor
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

### 1.2 application.yml 구성

```yaml
spring:
  application:
    name: Memora-Server

  datasource:
    url: jdbc:postgresql://localhost:5432/memora
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

# JWT
jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 3600000    # 1시간
  refresh-token-expiration: 604800000  # 7일

# AI Server
ai:
  server:
    url: http://localhost:8000

# AWS S3
cloud:
  aws:
    s3:
      bucket: memora-uploads
    region:
      static: ap-northeast-2
    credentials:
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
```

### 1.3 공통 응답 구조 정의

```java
// 공통 응답 래퍼
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}

// 페이지네이션 응답
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}

// 에러 응답
public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private LocalDateTime timestamp;
}
```

### 1.4 전역 예외 처리

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // BusinessException, ValidationException, AuthException 등 처리
}
```

### 체크리스트
- [ ] build.gradle 의존성 추가
- [ ] application.yml 작성 (dev/prod 프로파일 분리)
- [ ] 공통 응답 구조 (ApiResponse, ErrorResponse)
- [ ] 전역 예외 핸들러
- [ ] CORS 설정
- [ ] 로깅 설정 (logback-spring.xml)
- [ ] .env 파일 및 .gitignore 설정

---

## STEP 2: DB 설계 및 엔티티 구현

### 2.1 ERD 개요

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────┐
│    User      │────▶│  Enrollment  │◀────│     Course       │
│             │     └──────────────┘     │                  │
└──────┬──────┘                          └────────┬─────────┘
       │                                          │
       │     ┌──────────────┐            ┌────────▼─────────┐
       │     │  QaSession   │            │    Lecture        │
       │────▶│              │            │                  │
       │     └──────┬───────┘            └────────┬─────────┘
       │            │                             │
       │     ┌──────▼───────┐            ┌────────▼─────────┐
       │     │  QaMessage   │            │   Document       │
       │     └──────────────┘            │                  │
       │                                 └────────┬─────────┘
       │                                          │
       │     ┌──────────────┐            ┌────────▼─────────┐
       │────▶│  QuizAttempt │◀───────────│   Quiz           │
       │     └──────────────┘            │                  │
       │                                 └──────────────────┘
       │
       │     ┌──────────────┐
       └────▶│ LearningLog  │
             └──────────────┘
```

### 2.2 테이블 상세

#### users
```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'STUDENT',  -- STUDENT, INSTRUCTOR, ADMIN
    profile_image   VARCHAR(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### courses
```sql
CREATE TABLE courses (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    instructor_id   BIGINT REFERENCES users(id),
    thumbnail_url   VARCHAR(500),
    status          VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, ARCHIVED
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### enrollments
```sql
CREATE TABLE enrollments (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    course_id       BIGINT REFERENCES courses(id),
    role            VARCHAR(20) DEFAULT 'STUDENT',  -- STUDENT, ASSISTANT
    enrolled_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, course_id)
);
```

#### lectures
```sql
CREATE TABLE lectures (
    id              BIGSERIAL PRIMARY KEY,
    course_id       BIGINT REFERENCES courses(id),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    order_index     INT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### documents
```sql
CREATE TABLE documents (
    id              BIGSERIAL PRIMARY KEY,
    lecture_id      BIGINT REFERENCES lectures(id),
    original_name   VARCHAR(255) NOT NULL,
    stored_path     VARCHAR(500) NOT NULL,       -- S3 경로
    file_type       VARCHAR(20) NOT NULL,         -- PDF, TXT, DOCX
    file_size       BIGINT,
    summary         TEXT,                          -- AI 요약
    chunk_count     INT DEFAULT 0,
    processing_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### document_chunks
```sql
CREATE TABLE document_chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT REFERENCES documents(id),
    chunk_index     INT NOT NULL,
    content         TEXT NOT NULL,
    page_number     INT,
    token_count     INT,
    embedding_id    VARCHAR(255),                  -- FAISS 벡터 ID
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### qa_sessions
```sql
CREATE TABLE qa_sessions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    lecture_id      BIGINT REFERENCES lectures(id),
    title           VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### qa_messages
```sql
CREATE TABLE qa_messages (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT REFERENCES qa_sessions(id),
    role            VARCHAR(20) NOT NULL,          -- USER, ASSISTANT
    content         TEXT NOT NULL,
    source_chunks   TEXT,                           -- 참조한 chunk ID 목록 (JSON)
    token_used      INT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### quizzes
```sql
CREATE TABLE quizzes (
    id              BIGSERIAL PRIMARY KEY,
    lecture_id      BIGINT REFERENCES lectures(id),
    question        TEXT NOT NULL,
    quiz_type       VARCHAR(20) NOT NULL,          -- MULTIPLE_CHOICE, SHORT_ANSWER, ESSAY
    options         TEXT,                           -- JSON: 객관식 선택지
    correct_answer  TEXT NOT NULL,
    explanation     TEXT,                           -- 해설
    difficulty      VARCHAR(10) DEFAULT 'MEDIUM',  -- EASY, MEDIUM, HARD
    concept_tag     VARCHAR(255),                   -- 관련 개념 태그
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### quiz_attempts
```sql
CREATE TABLE quiz_attempts (
    id              BIGSERIAL PRIMARY KEY,
    quiz_id         BIGINT REFERENCES quizzes(id),
    user_id         BIGINT REFERENCES users(id),
    user_answer     TEXT NOT NULL,
    is_correct      BOOLEAN,
    score           INT,                           -- 서술형은 점수
    ai_feedback     TEXT,                          -- AI 피드백
    time_spent      INT,                           -- 소요시간(초)
    attempted_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### learning_logs
```sql
CREATE TABLE learning_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    lecture_id      BIGINT REFERENCES lectures(id),
    activity_type   VARCHAR(30) NOT NULL,          -- VIEW, QUESTION, QUIZ, REVIEW
    duration        INT,                           -- 활동 시간(초)
    metadata        TEXT,                           -- JSON: 추가 정보
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 체크리스트
- [ ] 모든 Entity 클래스 생성
- [ ] BaseEntity (id, createdAt, updatedAt) 추상 클래스
- [ ] Repository 인터페이스 생성
- [ ] 연관관계 매핑 확인
- [ ] 인덱스 설계 (자주 조회되는 컬럼)
- [ ] Flyway 또는 Liquibase 마이그레이션 (선택)

---

## STEP 3: 인증/인가 시스템

### 3.1 구현 범위

- 이메일/비밀번호 기반 회원가입 및 로그인
- JWT (Access Token + Refresh Token)
- Role 기반 권한 분리 (STUDENT, INSTRUCTOR, ADMIN)

### 3.2 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 (JWT 발급) |
| POST | `/api/auth/refresh` | 토큰 갱신 |
| GET | `/api/auth/me` | 내 정보 조회 |
| PUT | `/api/auth/me` | 내 정보 수정 |

### 3.3 구현 구조

```
security/
├── JwtTokenProvider.java       # JWT 생성/검증
├── JwtAuthenticationFilter.java # 요청마다 토큰 검증
├── SecurityConfig.java          # Spring Security 설정
├── CustomUserDetails.java       # UserDetails 구현
└── CustomUserDetailsService.java
```

### 체크리스트
- [ ] SecurityConfig 설정 (CORS, CSRF, 세션 정책)
- [ ] JWT 토큰 생성/검증 유틸
- [ ] 인증 필터 구현
- [ ] 회원가입/로그인 API
- [ ] Role enum 정의
- [ ] 비밀번호 BCrypt 암호화
- [ ] Swagger에서 JWT 인증 설정

---

## STEP 4: 강의 및 자료 관리 API

### 4.1 강의(Course) API

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/courses` | 강의 생성 | INSTRUCTOR |
| GET | `/api/courses` | 강의 목록 | ALL |
| GET | `/api/courses/{id}` | 강의 상세 | ENROLLED |
| PUT | `/api/courses/{id}` | 강의 수정 | INSTRUCTOR(소유) |
| DELETE | `/api/courses/{id}` | 강의 삭제 | INSTRUCTOR(소유) |
| POST | `/api/courses/{id}/enroll` | 수강 등록 | STUDENT |

### 4.2 강의자료(Lecture + Document) API

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/courses/{courseId}/lectures` | 차시 생성 | INSTRUCTOR |
| GET | `/api/courses/{courseId}/lectures` | 차시 목록 | ENROLLED |
| POST | `/api/lectures/{lectureId}/documents` | 자료 업로드 | INSTRUCTOR |
| GET | `/api/lectures/{lectureId}/documents` | 자료 목록 | ENROLLED |
| GET | `/api/documents/{id}/summary` | 문서 요약 조회 | ENROLLED |

### 4.3 파일 업로드 프로세스

```
1. 사용자가 PDF 업로드
2. Spring Boot → S3에 파일 저장
3. Document 엔티티 생성 (status: PENDING)
4. Spring Boot → FastAPI로 문서 처리 요청 (비동기)
5. FastAPI에서:
   a. S3에서 파일 다운로드
   b. PDF 텍스트 추출
   c. Chunking (500~1000 토큰 단위)
   d. 각 chunk 임베딩 생성
   e. FAISS에 벡터 저장
   f. 문서 요약 생성
6. FastAPI → Spring Boot 콜백 (처리 완료/실패)
7. Document status 업데이트 + chunk 정보 저장
```

### 체크리스트
- [ ] S3 파일 업로드/다운로드 서비스
- [ ] Course CRUD API + Service
- [ ] Lecture CRUD API + Service
- [ ] Document 업로드 API
- [ ] 비동기 AI 서버 호출 (WebClient)
- [ ] 콜백 수신 API
- [ ] 수강 등록/조회

---

## STEP 5: FastAPI AI 서버 구축

### 5.1 프로젝트 구조

```
memora-ai/
├── main.py                     # FastAPI 앱 엔트리포인트
├── requirements.txt
├── .env
├── app/
│   ├── __init__.py
│   ├── config.py               # 환경변수 관리
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── document.py         # 문서 처리 라우터
│   │   ├── qa.py               # 질의응답 라우터
│   │   ├── quiz.py             # 문제 생성 라우터
│   │   └── analysis.py         # 학습 분석 라우터
│   ├── services/
│   │   ├── __init__.py
│   │   ├── document_service.py # 문서 파싱/청킹
│   │   ├── embedding_service.py# 임베딩 생성/검색
│   │   ├── qa_service.py       # RAG 질의응답
│   │   ├── quiz_service.py     # 문제 생성
│   │   ├── grading_service.py  # 채점/피드백
│   │   └── analysis_service.py # 학습 분석
│   ├── prompts/
│   │   ├── __init__.py
│   │   ├── summary.py          # 요약 프롬프트
│   │   ├── qa.py               # 질의응답 프롬프트
│   │   ├── quiz.py             # 문제 생성 프롬프트
│   │   ├── grading.py          # 채점 프롬프트
│   │   └── analysis.py         # 분석 프롬프트
│   ├── models/
│   │   ├── __init__.py
│   │   └── schemas.py          # Pydantic 모델
│   └── utils/
│       ├── __init__.py
│       ├── pdf_parser.py       # PDF 텍스트 추출
│       └── text_splitter.py    # 텍스트 분할
├── vector_store/                # FAISS 인덱스 저장 디렉토리
└── Dockerfile
```

### 5.2 주요 의존성

```txt
fastapi==0.115.0
uvicorn==0.30.0
langchain==0.3.0
langchain-openai==0.2.0
langchain-community==0.3.0
faiss-cpu==1.8.0
openai==1.50.0
python-multipart==0.0.12
PyPDF2==3.0.1
tiktoken==0.7.0
pydantic==2.9.0
httpx==0.27.0
python-dotenv==1.0.1
boto3==1.35.0
```

### 5.3 FastAPI 엔드포인트 (Spring Boot에서 호출)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/ai/documents/process` | 문서 파싱 + 임베딩 + 요약 |
| POST | `/ai/qa/ask` | 질의응답 (RAG) |
| POST | `/ai/quiz/generate` | 문제 생성 |
| POST | `/ai/quiz/grade` | 채점 + 피드백 |
| POST | `/ai/analysis/learning` | 학습 분석 |
| GET | `/ai/health` | 헬스체크 |

### 체크리스트
- [ ] FastAPI 프로젝트 초기화
- [ ] 환경변수 관리 (.env)
- [ ] 라우터 구조 설정
- [ ] Pydantic 스키마 정의
- [ ] 헬스체크 API
- [ ] Spring Boot ↔ FastAPI 통신 테스트
- [ ] 에러 핸들링 미들웨어

---

## STEP 6: RAG 파이프라인 구현

### 6.1 문서 처리 파이프라인

```
PDF 업로드
    ↓
텍스트 추출 (PyPDF2)
    ↓
텍스트 전처리 (불필요 문자 제거, 정규화)
    ↓
Chunking (RecursiveCharacterTextSplitter)
  - chunk_size: 800 토큰
  - chunk_overlap: 100 토큰
    ↓
각 Chunk 임베딩 생성 (OpenAI text-embedding-3-small)
    ↓
FAISS 인덱스에 저장
    ↓
문서 요약 생성 (GPT-4o-mini)
    ↓
Spring Boot에 결과 콜백
```

### 6.2 질의응답 파이프라인 (RAG)

```
사용자 질문 입력
    ↓
질문 임베딩 생성
    ↓
FAISS에서 유사 chunk 검색 (top_k=5)
    ↓
검색된 chunk + 질문을 프롬프트에 조합
    ↓
GPT-4o 응답 생성
    ↓
출처(chunk 정보) 포함하여 반환
```

### 6.3 핵심 구현 코드 구조

```python
# embedding_service.py
class EmbeddingService:
    def __init__(self):
        self.embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
        self.vector_stores = {}  # lecture_id별 FAISS 인덱스

    def add_documents(self, lecture_id: str, chunks: list[str], metadatas: list[dict]):
        """청크를 임베딩하여 FAISS에 저장"""

    def search(self, lecture_id: str, query: str, top_k: int = 5) -> list[dict]:
        """유사 문서 검색"""

    def save_index(self, lecture_id: str):
        """FAISS 인덱스를 디스크에 저장"""

    def load_index(self, lecture_id: str):
        """FAISS 인덱스를 디스크에서 로드"""
```

### 6.4 FAISS 인덱스 관리 전략

- lecture_id 단위로 인덱스 분리 → 검색 범위 제한
- 디스크 저장/로드로 서버 재시작 시 복구
- MVP에서는 메모리 기반 + 주기적 디스크 저장

### 체크리스트
- [ ] PDF 텍스트 추출 유틸
- [ ] RecursiveCharacterTextSplitter 적용
- [ ] OpenAI 임베딩 서비스
- [ ] FAISS 인덱스 생성/저장/로드
- [ ] 유사도 검색 기능
- [ ] RAG 체인 구현 (LangChain)
- [ ] 문서 요약 기능
- [ ] 콜백 API (처리 완료 시 Spring Boot에 알림)

---

## STEP 7: AI 질의응답 API

### 7.1 전체 흐름

```
[프론트] 질문 입력
    ↓
[Spring Boot] POST /api/qa/sessions/{sessionId}/messages
    ↓ (질문 저장 + AI 서버 호출)
[FastAPI] POST /ai/qa/ask
    ↓ (RAG 파이프라인)
[FastAPI] 응답 반환 (답변 + 출처)
    ↓
[Spring Boot] 응답 저장 + 프론트에 반환
    ↓
[프론트] 답변 표시 (출처 하이라이트 포함)
```

### 7.2 Spring Boot API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/lectures/{lectureId}/qa/sessions` | QA 세션 생성 |
| GET | `/api/qa/sessions` | 내 QA 세션 목록 |
| GET | `/api/qa/sessions/{sessionId}` | 세션 상세 (메시지 포함) |
| POST | `/api/qa/sessions/{sessionId}/messages` | 질문 전송 |
| DELETE | `/api/qa/sessions/{sessionId}` | 세션 삭제 |

### 7.3 요청/응답 형식

```json
// POST /api/qa/sessions/{sessionId}/messages - Request
{
    "content": "RAG에서 retrieval은 어떤 역할을 하나요?",
    "difficulty": "MEDIUM"  // EASY, MEDIUM, HARD (선택)
}

// Response
{
    "success": true,
    "data": {
        "id": 42,
        "role": "ASSISTANT",
        "content": "RAG(Retrieval-Augmented Generation)에서 Retrieval은...",
        "sources": [
            {
                "documentName": "AI개론_3장.pdf",
                "pageNumber": 12,
                "chunkPreview": "Retrieval은 사용자의 질의와 관련된..."
            }
        ],
        "createdAt": "2026-04-06T14:30:00"
    }
}
```

### 체크리스트
- [ ] QA 세션 CRUD API
- [ ] 메시지 전송 API (Spring Boot → FastAPI)
- [ ] 컨텍스트 유지 (이전 대화 포함)
- [ ] 출처 정보 반환
- [ ] 난이도 조절 파라미터

---

## STEP 8: 문제 생성 및 채점 시스템

### 8.1 문제 생성 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/lectures/{lectureId}/quizzes/generate` | 문제 자동 생성 |
| GET | `/api/lectures/{lectureId}/quizzes` | 문제 목록 |
| GET | `/api/quizzes/{quizId}` | 문제 상세 |
| POST | `/api/quizzes/{quizId}/submit` | 답안 제출 + 채점 |
| GET | `/api/quizzes/{quizId}/attempts` | 풀이 기록 |

### 8.2 문제 생성 요청

```json
// POST /api/lectures/{lectureId}/quizzes/generate - Request
{
    "count": 5,
    "types": ["MULTIPLE_CHOICE", "SHORT_ANSWER"],
    "difficulty": "MEDIUM",
    "conceptTags": ["RAG", "임베딩"]  // 선택: 특정 개념 지정
}

// Response
{
    "success": true,
    "data": [
        {
            "id": 1,
            "question": "RAG에서 Retrieval 단계의 주요 목적은?",
            "quizType": "MULTIPLE_CHOICE",
            "options": [
                "A. 모델을 학습시키기 위해",
                "B. 관련 문서를 검색하기 위해",
                "C. 응답을 생성하기 위해",
                "D. 데이터를 전처리하기 위해"
            ],
            "difficulty": "MEDIUM",
            "conceptTag": "RAG"
        }
    ]
}
```

### 8.3 채점 + 피드백

```json
// POST /api/quizzes/{quizId}/submit - Request
{
    "userAnswer": "B"
}

// Response
{
    "success": true,
    "data": {
        "isCorrect": true,
        "correctAnswer": "B",
        "score": 100,
        "explanation": "RAG의 Retrieval 단계는 사용자 질의와 관련된 문서를 검색하여...",
        "aiFeedback": "정확하게 이해하고 있습니다. 추가로 Retrieval 과정에서 사용되는..."
    }
}
```

### 체크리스트
- [ ] 문제 자동 생성 API (Spring → FastAPI)
- [ ] 문제 유형별 생성 로직
- [ ] 답안 제출 API
- [ ] 자동 채점 (객관식/주관식)
- [ ] AI 피드백 생성
- [ ] 풀이 기록 저장

---

## STEP 9: 학습 분석 시스템

### 9.1 분석 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/analysis/me` | 내 종합 학습 분석 |
| GET | `/api/analysis/me/courses/{courseId}` | 강의별 분석 |
| GET | `/api/analysis/courses/{courseId}/students` | 학생별 현황 (교강사) |
| GET | `/api/analysis/courses/{courseId}/overview` | 강의 전체 분석 (교강사) |

### 9.2 수강생 분석 데이터 구조

```json
{
    "userId": 1,
    "courseId": 10,
    "overallScore": 78,
    "totalStudyTime": 4200,
    "quizStats": {
        "totalAttempts": 25,
        "correctRate": 0.72,
        "averageTimePerQuiz": 45
    },
    "weakConcepts": [
        {"concept": "벡터 임베딩", "correctRate": 0.4, "attemptCount": 5},
        {"concept": "어텐션 메커니즘", "correctRate": 0.5, "attemptCount": 4}
    ],
    "studyRecommendations": [
        "벡터 임베딩에 대한 복습이 필요합니다. Lecture 3을 다시 학습해보세요.",
        "어텐션 메커니즘 관련 문제를 추가로 풀어보세요."
    ],
    "weeklyProgress": [
        {"week": "2026-W14", "studyTime": 1200, "quizScore": 65},
        {"week": "2026-W15", "studyTime": 1800, "quizScore": 78}
    ]
}
```

### 9.3 교강사 대시보드 데이터

```json
{
    "courseId": 10,
    "totalStudents": 35,
    "averageScore": 72,
    "completionRate": 0.85,
    "topWeakConcepts": [
        {"concept": "벡터 임베딩", "avgCorrectRate": 0.45},
        {"concept": "트랜스포머", "avgCorrectRate": 0.52}
    ],
    "frequentQuestions": [
        {"question": "임베딩 차원은 어떻게 정하나요?", "count": 12},
        {"question": "셀프 어텐션과 크로스 어텐션의 차이?", "count": 8}
    ],
    "studentDistribution": {
        "excellent": 5,
        "good": 15,
        "average": 10,
        "needsHelp": 5
    }
}
```

### 체크리스트
- [ ] 학습 로그 수집 API
- [ ] 수강생 개인 분석 API
- [ ] 강의별 분석 API
- [ ] 취약 개념 도출 로직
- [ ] 학습 추천 생성 (AI)
- [ ] 교강사 대시보드 데이터 API

---

## STEP 10: 통합 테스트 및 배포

### 10.1 Docker 구성

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Spring Boot 백엔드
  backend:
    build:
      context: ./Memora_Server
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - AI_SERVER_URL=http://ai-server:8000
    depends_on:
      - postgres
      - ai-server

  # FastAPI AI 서버
  ai-server:
    build:
      context: ./memora-ai
      dockerfile: Dockerfile
    ports:
      - "8000:8000"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - CALLBACK_URL=http://backend:8080
    volumes:
      - vector_data:/app/vector_store

  # PostgreSQL
  postgres:
    image: postgres:16
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=memora
      - POSTGRES_USER=${DB_USERNAME}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
  vector_data:
```

### 10.2 Spring Boot Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/Memora_Server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 10.3 FastAPI Dockerfile

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 10.4 AWS 배포 구조 (향후)

```
AWS 구성:
├── EC2 (t3.medium) - Docker Compose로 전체 구동
├── RDS (PostgreSQL) - 프로덕션 DB
├── S3 - 파일 저장
└── Route 53 - 도메인 연결
```

### 체크리스트
- [ ] Dockerfile 작성 (Spring Boot, FastAPI)
- [ ] docker-compose.yml 작성
- [ ] 환경변수 관리 (.env)
- [ ] 로컬 Docker 실행 테스트
- [ ] API 통합 테스트
- [ ] AWS EC2 배포 (선택)

---

## DB 스키마 설계

### ER 다이어그램 (요약)

```
users (1) ──── (N) enrollments (N) ──── (1) courses
  │                                          │
  │                                    (1) ──── (N) lectures
  │                                                   │
  │                                          (1) ──── (N) documents
  │                                          │              │
  │                                          │         (1) ──── (N) document_chunks
  │                                          │
  │    (1) ──── (N) qa_sessions ──── (1) lectures
  │                    │
  │              (1) ──── (N) qa_messages
  │
  │                              lectures ──── (1) ── (N) quizzes
  │                                                        │
  │    (1) ──── (N) quiz_attempts ──── (N) ──── (1) quizzes
  │
  │    (1) ──── (N) learning_logs
```

### 인덱스 전략

```sql
-- 자주 사용되는 조회 패턴에 인덱스
CREATE INDEX idx_enrollment_user ON enrollments(user_id);
CREATE INDEX idx_enrollment_course ON enrollments(course_id);
CREATE INDEX idx_lecture_course ON lectures(course_id);
CREATE INDEX idx_document_lecture ON documents(lecture_id);
CREATE INDEX idx_chunk_document ON document_chunks(document_id);
CREATE INDEX idx_qa_session_user ON qa_sessions(user_id);
CREATE INDEX idx_qa_session_lecture ON qa_sessions(lecture_id);
CREATE INDEX idx_qa_message_session ON qa_messages(session_id);
CREATE INDEX idx_quiz_lecture ON quizzes(lecture_id);
CREATE INDEX idx_quiz_attempt_user ON quiz_attempts(user_id);
CREATE INDEX idx_quiz_attempt_quiz ON quiz_attempts(quiz_id);
CREATE INDEX idx_learning_log_user ON learning_logs(user_id);
CREATE INDEX idx_learning_log_lecture ON learning_logs(lecture_id);
CREATE INDEX idx_learning_log_created ON learning_logs(created_at);
```

---

## API 명세 요약

### 인증

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/signup` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/refresh` | 토큰 갱신 | X |
| GET | `/api/auth/me` | 내 정보 | O |

### 강의 관리

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/courses` | 강의 생성 | INSTRUCTOR |
| GET | `/api/courses` | 강의 목록 | ALL |
| GET | `/api/courses/{id}` | 강의 상세 | ALL |
| PUT | `/api/courses/{id}` | 강의 수정 | INSTRUCTOR |
| DELETE | `/api/courses/{id}` | 강의 삭제 | INSTRUCTOR |
| POST | `/api/courses/{id}/enroll` | 수강 등록 | STUDENT |

### 강의 자료

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/courses/{cId}/lectures` | 차시 생성 | INSTRUCTOR |
| GET | `/api/courses/{cId}/lectures` | 차시 목록 | ENROLLED |
| POST | `/api/lectures/{lId}/documents` | 자료 업로드 | INSTRUCTOR |
| GET | `/api/lectures/{lId}/documents` | 자료 목록 | ENROLLED |
| GET | `/api/documents/{id}/summary` | 문서 요약 | ENROLLED |

### AI 질의응답

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/lectures/{lId}/qa/sessions` | QA 세션 생성 | ENROLLED |
| GET | `/api/qa/sessions` | 내 세션 목록 | STUDENT |
| GET | `/api/qa/sessions/{sId}` | 세션 상세 | 소유자 |
| POST | `/api/qa/sessions/{sId}/messages` | 질문 전송 | 소유자 |

### 문제/채점

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/lectures/{lId}/quizzes/generate` | 문제 생성 | ENROLLED |
| GET | `/api/lectures/{lId}/quizzes` | 문제 목록 | ENROLLED |
| POST | `/api/quizzes/{qId}/submit` | 답안 제출 | ENROLLED |
| GET | `/api/quizzes/{qId}/attempts` | 풀이 기록 | 소유자 |

### 학습 분석

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| GET | `/api/analysis/me` | 내 종합 분석 | STUDENT |
| GET | `/api/analysis/me/courses/{cId}` | 강의별 분석 | STUDENT |
| GET | `/api/analysis/courses/{cId}/students` | 학생별 현황 | INSTRUCTOR |
| GET | `/api/analysis/courses/{cId}/overview` | 강의 전체 분석 | INSTRUCTOR |

---

## 프롬프트 설계

### 1. 문서 요약 프롬프트

```
당신은 교육 콘텐츠 전문가입니다.
아래 강의 자료를 분석하여 학습에 도움이 되는 구조화된 요약을 생성하세요.

[문서 내용]
{document_text}

다음 형식으로 요약하세요:
1. **핵심 개요** (2-3문장)
2. **주요 개념** (bullet point)
3. **핵심 키워드** (쉼표로 구분)
4. **학습 목표** (이 자료를 통해 배울 수 있는 것)
```

### 2. 질의응답 (RAG) 프롬프트

```
당신은 친절하고 정확한 학습 도우미입니다.
아래 강의 자료의 관련 부분을 참고하여 학생의 질문에 답변하세요.

[참고 자료]
{retrieved_chunks}

[이전 대화]
{chat_history}

[학생 질문]
{question}

[난이도: {difficulty}]

규칙:
- 참고 자료에 있는 내용을 기반으로 정확하게 답변하세요.
- 자료에 없는 내용은 "제공된 자료에는 해당 내용이 없습니다"라고 안내하세요.
- {difficulty}에 맞는 수준으로 설명하세요.
  - EASY: 비유와 예시를 많이 사용하여 쉽게 설명
  - MEDIUM: 핵심 개념을 정확하게 설명
  - HARD: 심화 내용과 연관 개념까지 포함
- 출처를 반드시 명시하세요.
```

### 3. 문제 생성 프롬프트

```
당신은 교육 평가 전문가입니다.
아래 강의 내용을 기반으로 학습 확인용 문제를 생성하세요.

[강의 내용]
{lecture_content}

[요청 사항]
- 문제 수: {count}개
- 유형: {quiz_types}
- 난이도: {difficulty}
- 관련 개념: {concept_tags}

각 문제는 다음 JSON 형식으로 출력하세요:
{
    "question": "문제 텍스트",
    "quizType": "MULTIPLE_CHOICE | SHORT_ANSWER | ESSAY",
    "options": ["A. ...", "B. ...", "C. ...", "D. ..."],  // 객관식만
    "correctAnswer": "정답",
    "explanation": "해설",
    "conceptTag": "관련 개념"
}

규칙:
- 단순 암기가 아닌 이해를 확인하는 문제를 출제하세요.
- 선택지는 그럴듯한 오답을 포함하세요.
- 해설은 왜 정답인지, 왜 오답인지 설명하세요.
```

### 4. 채점/피드백 프롬프트

```
당신은 교육 전문 채점관입니다.
학생의 답안을 평가하고 학습에 도움이 되는 피드백을 제공하세요.

[문제]
{question}

[정답]
{correct_answer}

[학생 답안]
{user_answer}

[문제 유형: {quiz_type}]

다음 JSON 형식으로 응답하세요:
{
    "isCorrect": true/false,
    "score": 0-100,
    "feedback": "구체적인 피드백",
    "improvement": "개선 방향 제안"
}

규칙:
- 서술형은 핵심 키워드 포함 여부와 논리적 완성도로 평가하세요.
- 부분 점수를 허용하세요.
- 틀린 부분은 왜 틀렸는지 친절하게 설명하세요.
- 잘한 부분은 인정하고 격려하세요.
```

### 5. 학습 분석 프롬프트

```
당신은 학습 데이터 분석 전문가입니다.
아래 학생의 학습 데이터를 분석하여 맞춤형 학습 조언을 생성하세요.

[학습 데이터]
- 퀴즈 정답률: {correct_rate}
- 취약 개념: {weak_concepts}
- 질문 패턴: {question_patterns}
- 학습 시간 추이: {study_time_trend}
- 최근 성적 변화: {score_trend}

다음을 분석하세요:
1. **현재 학습 상태 진단** (강점과 약점)
2. **취약 개념 분석** (왜 어려워하는지 추정)
3. **맞춤형 학습 추천** (구체적인 행동 제안 3가지)
4. **동기 부여 메시지**
```

---

## 디렉토리 구조

### Spring Boot (백엔드)

```
src/main/java/com/kit/memora_server/
├── MemoraServerApplication.java
├── global/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebClientConfig.java
│   │   ├── SwaggerConfig.java
│   │   ├── CorsConfig.java
│   │   └── S3Config.java
│   ├── common/
│   │   ├── ApiResponse.java
│   │   ├── PageResponse.java
│   │   └── ErrorResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BusinessException.java
│   │   ├── ErrorCode.java
│   │   └── ...
│   └── security/
│       ├── JwtTokenProvider.java
│       ├── JwtAuthenticationFilter.java
│       ├── CustomUserDetails.java
│       └── CustomUserDetailsService.java
├── domain/
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── dto/
│   │   │   ├── SignupRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   └── TokenResponse.java
│   │   └── service/AuthService.java
│   ├── user/
│   │   ├── entity/User.java
│   │   ├── repository/UserRepository.java
│   │   └── enums/UserRole.java
│   ├── course/
│   │   ├── controller/CourseController.java
│   │   ├── dto/
│   │   ├── entity/Course.java
│   │   ├── entity/Enrollment.java
│   │   ├── repository/
│   │   └── service/CourseService.java
│   ├── lecture/
│   │   ├── controller/LectureController.java
│   │   ├── dto/
│   │   ├── entity/Lecture.java
│   │   ├── repository/
│   │   └── service/LectureService.java
│   ├── document/
│   │   ├── controller/DocumentController.java
│   │   ├── dto/
│   │   ├── entity/Document.java
│   │   ├── entity/DocumentChunk.java
│   │   ├── repository/
│   │   └── service/
│   │       ├── DocumentService.java
│   │       └── S3Service.java
│   ├── qa/
│   │   ├── controller/QaController.java
│   │   ├── dto/
│   │   ├── entity/QaSession.java
│   │   ├── entity/QaMessage.java
│   │   ├── repository/
│   │   └── service/QaService.java
│   ├── quiz/
│   │   ├── controller/QuizController.java
│   │   ├── dto/
│   │   ├── entity/Quiz.java
│   │   ├── entity/QuizAttempt.java
│   │   ├── repository/
│   │   └── service/QuizService.java
│   └── analysis/
│       ├── controller/AnalysisController.java
│       ├── dto/
│       ├── entity/LearningLog.java
│       ├── repository/
│       └── service/AnalysisService.java
└── infra/
    └── ai/
        ├── AiServerClient.java       # FastAPI 호출 WebClient
        ├── dto/
        │   ├── AiQaRequest.java
        │   ├── AiQaResponse.java
        │   ├── AiQuizGenerateRequest.java
        │   └── ...
        └── AiCallbackController.java  # AI 서버 콜백 수신
```

---

## MVP 범위 재확인

### 반드시 포함 (P0)
- 회원가입/로그인 (JWT)
- 강의 생성/조회
- PDF 업로드 및 AI 문서 처리
- 문서 요약 조회
- AI 질의응답 (RAG)
- 기본 문제 생성 (객관식)
- 기본 채점

### 가능하면 포함 (P1)
- 서술형 문제 생성/채점
- 학습 분석 대시보드
- 학습 추천
- 교강사 학생 현황

### 제외 (MVP 이후)
- OAuth 소셜 로그인
- 실시간 알림 (WebSocket)
- 협업 기능
- 외부 LMS 연동
- 결제 시스템
