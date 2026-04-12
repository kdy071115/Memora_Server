# Memora 백엔드 개발 계획서

> 최종 수정: 2026-04-06
> Spring Boot 3.5.13 / Java 17 / FastAPI / PostgreSQL / FAISS

---

## 전체 아키텍처

```
┌─────────────┐       ┌─────────────────┐       ┌──────────────────┐
│  Frontend   │──REST──│  Spring Boot    │──HTTP──│  FastAPI (AI)    │
│  (Next.js)  │◀──JSON─│  :8080          │◀──────│  :8000           │
└─────────────┘       └───────┬─────────┘       └───────┬──────────┘
                              │                          │
                     ┌────────▼────────┐       ┌─────────▼─────────┐
                     │  PostgreSQL     │       │  FAISS (Vector)   │
                     │  (AWS RDS)      │       │  + OpenAI API     │
                     └─────────────────┘       └───────────────────┘
                              │
                     ┌────────▼────────┐
                     │  AWS S3         │
                     │  (파일 저장)     │
                     └─────────────────┘
```

**통신 흐름:**
1. 프론트 → Spring Boot: REST API (JSON)
2. Spring Boot → FastAPI: WebClient 비동기 호출
3. FastAPI → OpenAI: LangChain 기반 GPT 호출
4. FastAPI → FAISS: 벡터 임베딩/검색
5. Spring Boot → PostgreSQL: Spring Data JPA
6. Spring Boot → S3: AWS SDK 파일 업/다운로드

---

## 개발 단계 요약

| STEP | 내용 | 우선순위 |
|------|------|---------|
| 1 | 프로젝트 기반 세팅 | P0 |
| 2 | DB 설계 + 엔티티 구현 | P0 |
| 3 | 인증/인가 (JWT) | P0 |
| 4 | 강의/자료 관리 API | P0 |
| 5 | FastAPI AI 서버 구축 | P0 |
| 6 | RAG 파이프라인 (문서처리 + 벡터검색) | P0 |
| 7 | AI 질의응답 API | P0 |
| 8 | 문제 생성 + 채점 | P1 |
| 9 | 학습 분석 | P1 |
| 10 | Docker 배포 + 통합 테스트 | P0 |

---

## STEP 1. 프로젝트 기반 세팅

### 할 일

#### 1-1. build.gradle 의존성 추가

```gradle
dependencies {
    // Web
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    // JPA + PostgreSQL
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

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

#### 1-2. application.yml 작성

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/memora
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

jwt:
  secret: ${JWT_SECRET}
  access-expiration: 3600000
  refresh-expiration: 604800000

ai:
  server:
    url: http://localhost:8000

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

#### 1-3. 공통 구조 생성

- `ApiResponse<T>` — 통일된 응답 래퍼 (`success`, `message`, `data`)
- `ErrorResponse` — 에러 응답 (`status`, `code`, `message`, `timestamp`)
- `GlobalExceptionHandler` — `@RestControllerAdvice` 전역 예외 처리
- CORS 설정 (`http://localhost:3000` 허용)

### 체크리스트

- [ ] build.gradle 의존성 추가
- [ ] application.yml (dev 프로파일)
- [ ] ApiResponse / ErrorResponse 클래스
- [ ] GlobalExceptionHandler
- [ ] CORS 설정
- [ ] .env + .gitignore 정리

---

## STEP 2. DB 설계 + 엔티티 구현

### ERD

```
users ─────┬──< enrollments >──── courses
            │                        │
            │                   lectures
            │                     │    │
            │               documents  quizzes
            │                  │          │
            │          document_chunks  quiz_attempts ──┐
            │                                           │
            ├──< qa_sessions                            │
            │        │                                  │
            │    qa_messages                             │
            │                                           │
            └──< learning_logs >────────────────────────┘
```

### 테이블 설계

#### users
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| email | VARCHAR(255) UNIQUE | 로그인 이메일 |
| password | VARCHAR(255) | BCrypt 암호화 |
| name | VARCHAR(100) | 이름 |
| role | VARCHAR(20) | STUDENT / INSTRUCTOR / ADMIN |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

#### courses
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| title | VARCHAR(255) | 강의명 |
| description | TEXT | 설명 |
| instructor_id | BIGINT FK → users | 교강사 |
| status | VARCHAR(20) | ACTIVE / ARCHIVED |
| created_at | TIMESTAMP | |

#### enrollments
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| course_id | BIGINT FK → courses | |
| enrolled_at | TIMESTAMP | |
| UNIQUE(user_id, course_id) | | |

#### lectures
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| course_id | BIGINT FK → courses | |
| title | VARCHAR(255) | 차시 제목 |
| description | TEXT | |
| order_index | INT | 순서 |
| created_at | TIMESTAMP | |

#### documents
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| lecture_id | BIGINT FK → lectures | |
| original_name | VARCHAR(255) | 원본 파일명 |
| stored_path | VARCHAR(500) | S3 경로 |
| file_type | VARCHAR(20) | PDF / TXT / DOCX |
| file_size | BIGINT | |
| summary | TEXT | AI 생성 요약 |
| chunk_count | INT | 청크 수 |
| processing_status | VARCHAR(20) | PENDING / PROCESSING / COMPLETED / FAILED |
| created_at | TIMESTAMP | |

#### document_chunks
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| document_id | BIGINT FK → documents | |
| chunk_index | INT | 순서 |
| content | TEXT | 청크 텍스트 |
| page_number | INT | 페이지 번호 |
| token_count | INT | |
| embedding_id | VARCHAR(255) | FAISS 벡터 ID |

#### qa_sessions
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| lecture_id | BIGINT FK → lectures | |
| title | VARCHAR(255) | 세션 제목 |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

#### qa_messages
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| session_id | BIGINT FK → qa_sessions | |
| role | VARCHAR(20) | USER / ASSISTANT |
| content | TEXT | 메시지 본문 |
| source_chunks | TEXT | 참조 chunk ID (JSON) |
| created_at | TIMESTAMP | |

#### quizzes
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| lecture_id | BIGINT FK → lectures | |
| question | TEXT | 문제 |
| quiz_type | VARCHAR(20) | MULTIPLE_CHOICE / SHORT_ANSWER / ESSAY |
| options | TEXT (JSON) | 객관식 선택지 |
| correct_answer | TEXT | 정답 |
| explanation | TEXT | 해설 |
| difficulty | VARCHAR(10) | EASY / MEDIUM / HARD |
| concept_tag | VARCHAR(255) | 관련 개념 |
| created_at | TIMESTAMP | |

#### quiz_attempts
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| quiz_id | BIGINT FK → quizzes | |
| user_id | BIGINT FK → users | |
| user_answer | TEXT | |
| is_correct | BOOLEAN | |
| score | INT | 점수 (서술형) |
| ai_feedback | TEXT | AI 피드백 |
| time_spent | INT | 소요시간(초) |
| attempted_at | TIMESTAMP | |

#### learning_logs
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| lecture_id | BIGINT FK → lectures | |
| activity_type | VARCHAR(30) | VIEW / QUESTION / QUIZ / REVIEW |
| duration | INT | 활동시간(초) |
| metadata | TEXT (JSON) | 추가정보 |
| created_at | TIMESTAMP | |

### 체크리스트

- [ ] BaseEntity (id, createdAt, updatedAt) 추상 클래스
- [ ] 모든 Entity 클래스 작성
- [ ] Repository 인터페이스 생성
- [ ] UserRole enum (STUDENT, INSTRUCTOR, ADMIN)
- [ ] 연관관계 매핑 확인

---

## STEP 3. 인증/인가 (JWT)

### API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/signup` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인 → JWT 발급 | X |
| POST | `/api/auth/refresh` | 토큰 갱신 | X |
| GET | `/api/auth/me` | 내 정보 조회 | O |
| PUT | `/api/auth/me` | 내 정보 수정 | O |

### 구현 구조

```
global/security/
├── SecurityConfig.java              # Security 설정, 필터 체인
├── JwtTokenProvider.java            # JWT 생성/검증/파싱
├── JwtAuthenticationFilter.java     # 매 요청 토큰 검증 필터
├── CustomUserDetails.java           # UserDetails 구현
└── CustomUserDetailsService.java    # DB에서 사용자 로드
```

### 요청/응답

```json
// POST /api/auth/signup
{ "email": "student@kit.ac.kr", "password": "1234", "name": "김학생", "role": "STUDENT" }

// POST /api/auth/login → Response
{ "success": true, "data": { "accessToken": "eyJ...", "refreshToken": "eyJ..." } }
```

### 체크리스트

- [ ] SecurityConfig (CORS, CSRF off, stateless 세션)
- [ ] JwtTokenProvider (생성/검증)
- [ ] JwtAuthenticationFilter
- [ ] AuthController + AuthService
- [ ] 비밀번호 BCrypt 암호화
- [ ] Swagger JWT 인증 설정

---

## STEP 4. 강의/자료 관리 API

### 강의 API

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/courses` | 강의 생성 | INSTRUCTOR |
| GET | `/api/courses` | 강의 목록 | ALL |
| GET | `/api/courses/{id}` | 강의 상세 | ALL |
| PUT | `/api/courses/{id}` | 수정 | INSTRUCTOR(소유) |
| DELETE | `/api/courses/{id}` | 삭제 | INSTRUCTOR(소유) |
| POST | `/api/courses/{id}/enroll` | 수강 등록 | STUDENT |

### 차시 + 자료 API

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/courses/{cId}/lectures` | 차시 생성 | INSTRUCTOR |
| GET | `/api/courses/{cId}/lectures` | 차시 목록 | ENROLLED |
| POST | `/api/lectures/{lId}/documents` | **PDF 업로드** | INSTRUCTOR |
| GET | `/api/lectures/{lId}/documents` | 자료 목록 | ENROLLED |
| GET | `/api/documents/{id}/summary` | 문서 요약 | ENROLLED |

### PDF 업로드 처리 흐름

```
사용자가 PDF 업로드
    ↓
Spring Boot: S3 저장 + Document 엔티티 생성 (status: PENDING)
    ↓
Spring Boot → FastAPI: POST /ai/documents/process (비동기 WebClient)
    ↓
FastAPI:
  1. S3에서 파일 다운로드
  2. 텍스트 추출 (PyPDF2)
  3. Chunking (800토큰, 100토큰 오버랩)
  4. 임베딩 생성 (text-embedding-3-small)
  5. FAISS에 벡터 저장
  6. 요약 생성 (GPT-4o-mini)
    ↓
FastAPI → Spring Boot: POST /api/internal/documents/{id}/callback
    ↓
Spring Boot: Document 상태 업데이트 (COMPLETED) + 요약/청크 저장
```

### 체크리스트

- [ ] S3Service (업로드/다운로드/URL생성)
- [ ] CourseController + Service + CRUD
- [ ] LectureController + Service
- [ ] DocumentController (업로드 + 비동기 AI 호출)
- [ ] WebClient로 FastAPI 호출
- [ ] 콜백 수신 내부 API
- [ ] 수강 등록 기능

---

## STEP 5. FastAPI AI 서버 구축

### 프로젝트 구조

```
memora-ai/
├── main.py                      # FastAPI 엔트리포인트
├── requirements.txt
├── .env
├── Dockerfile
├── app/
│   ├── config.py                # 환경변수
│   ├── routers/
│   │   ├── document.py          # 문서 처리
│   │   ├── qa.py                # 질의응답
│   │   ├── quiz.py              # 문제 생성/채점
│   │   └── analysis.py          # 학습 분석
│   ├── services/
│   │   ├── document_service.py  # PDF 파싱 + 청킹
│   │   ├── embedding_service.py # 임베딩 + FAISS
│   │   ├── qa_service.py        # RAG 질의응답
│   │   ├── quiz_service.py      # 문제 생성
│   │   ├── grading_service.py   # 채점
│   │   └── analysis_service.py  # 분석
│   ├── prompts/
│   │   ├── summary.py           # 요약 프롬프트
│   │   ├── qa.py                # QA 프롬프트
│   │   ├── quiz.py              # 문제 생성 프롬프트
│   │   ├── grading.py           # 채점 프롬프트
│   │   └── analysis.py          # 분석 프롬프트
│   ├── models/
│   │   └── schemas.py           # Pydantic 모델
│   └── utils/
│       ├── pdf_parser.py
│       └── text_splitter.py
└── vector_store/                 # FAISS 인덱스 저장
```

### 주요 의존성 (requirements.txt)

```
fastapi==0.115.0
uvicorn==0.30.0
langchain==0.3.0
langchain-openai==0.2.0
langchain-community==0.3.0
faiss-cpu==1.8.0
openai==1.50.0
PyPDF2==3.0.1
tiktoken==0.7.0
httpx==0.27.0
python-dotenv==1.0.1
boto3==1.35.0
pydantic==2.9.0
```

### FastAPI 엔드포인트 (Spring Boot가 호출)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/ai/documents/process` | 문서 파싱 + 임베딩 + 요약 |
| POST | `/ai/qa/ask` | RAG 질의응답 |
| POST | `/ai/quiz/generate` | 문제 생성 |
| POST | `/ai/quiz/grade` | 채점 + 피드백 |
| POST | `/ai/analysis/learning` | 학습 분석 |
| GET | `/ai/health` | 헬스체크 |

### 체크리스트

- [ ] FastAPI 프로젝트 초기화
- [ ] .env (OPENAI_API_KEY, CALLBACK_URL 등)
- [ ] 라우터/서비스 구조 세팅
- [ ] Pydantic 스키마 정의
- [ ] 헬스체크 API
- [ ] Spring Boot ↔ FastAPI 통신 테스트

---

## STEP 6. RAG 파이프라인

### 문서 처리 파이프라인

```
PDF 업로드
  → 텍스트 추출 (PyPDF2)
  → 전처리 (불필요 문자 제거)
  → Chunking (RecursiveCharacterTextSplitter, 800토큰/100오버랩)
  → 임베딩 (OpenAI text-embedding-3-small)
  → FAISS 인덱스 저장 (lecture_id 단위 분리)
  → 요약 생성 (GPT-4o-mini)
  → Spring Boot 콜백
```

### 질의응답 파이프라인 (RAG)

```
사용자 질문
  → 질문 임베딩 생성
  → FAISS 유사도 검색 (top_k=5)
  → 검색된 chunk + 질문 → 프롬프트 조합
  → GPT-4o 응답 생성
  → 출처(chunk 정보) 포함 반환
```

### FAISS 관리 전략

- **lecture_id 단위로 인덱스 분리** → 검색 범위 제한, 정확도 향상
- 디스크 저장/로드 지원 (서버 재시작 복구)
- MVP에서는 메모리 + 주기적 디스크 백업

### 핵심 서비스 인터페이스

```python
class EmbeddingService:
    def add_documents(self, lecture_id: str, chunks: list[str], metadatas: list[dict])
    def search(self, lecture_id: str, query: str, top_k: int = 5) -> list[dict]
    def save_index(self, lecture_id: str)
    def load_index(self, lecture_id: str)
```

### 체크리스트

- [ ] PDF 텍스트 추출 (PyPDF2)
- [ ] RecursiveCharacterTextSplitter 적용
- [ ] OpenAI 임베딩 서비스
- [ ] FAISS 인덱스 생성/저장/로드
- [ ] 유사도 검색
- [ ] RAG 체인 (LangChain)
- [ ] 문서 요약 생성
- [ ] 콜백 API

---

## STEP 7. AI 질의응답 API

### 전체 흐름

```
프론트: 질문 입력
  → Spring Boot: POST /api/qa/sessions/{sId}/messages (질문 저장)
  → FastAPI: POST /ai/qa/ask (RAG)
  → Spring Boot: 응답 저장 + 프론트 반환
```

### Spring Boot QA API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/lectures/{lId}/qa/sessions` | QA 세션 생성 |
| GET | `/api/qa/sessions` | 내 세션 목록 |
| GET | `/api/qa/sessions/{sId}` | 세션 상세 (메시지 포함) |
| POST | `/api/qa/sessions/{sId}/messages` | 질문 전송 |
| DELETE | `/api/qa/sessions/{sId}` | 세션 삭제 |

### 요청/응답 예시

```json
// POST /api/qa/sessions/{sId}/messages
// Request
{ "content": "RAG에서 retrieval은 어떤 역할을 하나요?", "difficulty": "MEDIUM" }

// Response
{
  "success": true,
  "data": {
    "id": 42,
    "role": "ASSISTANT",
    "content": "RAG에서 Retrieval은 사용자 질의와 관련된 문서를...",
    "sources": [
      { "documentName": "AI개론_3장.pdf", "pageNumber": 12, "preview": "Retrieval은..." }
    ],
    "createdAt": "2026-04-06T14:30:00"
  }
}
```

### 체크리스트

- [ ] QA 세션 CRUD
- [ ] 메시지 전송 (Spring → FastAPI)
- [ ] 이전 대화 컨텍스트 전달
- [ ] 출처 정보 포함 응답
- [ ] 난이도 파라미터

---

## STEP 8. 문제 생성 + 채점

### API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/lectures/{lId}/quizzes/generate` | 문제 자동 생성 |
| GET | `/api/lectures/{lId}/quizzes` | 문제 목록 |
| GET | `/api/quizzes/{qId}` | 문제 상세 |
| POST | `/api/quizzes/{qId}/submit` | 답안 제출 + 채점 |
| GET | `/api/lectures/{lId}/quizzes/attempts` | 내 풀이 기록 |

### 요청/응답 예시

```json
// POST /api/lectures/{lId}/quizzes/generate
{ "count": 5, "types": ["MULTIPLE_CHOICE", "SHORT_ANSWER"], "difficulty": "MEDIUM" }

// POST /api/quizzes/{qId}/submit → Response
{
  "isCorrect": true,
  "correctAnswer": "B",
  "score": 100,
  "explanation": "RAG의 Retrieval은 관련 문서를 검색하여...",
  "aiFeedback": "정확하게 이해하고 있습니다."
}
```

### 체크리스트

- [ ] 문제 자동 생성 (Spring → FastAPI)
- [ ] 유형별 생성 (객관식/주관식/서술형)
- [ ] 답안 제출 API
- [ ] 자동 채점 (객관식: 정확 매칭 / 서술형: AI 채점)
- [ ] AI 피드백 생성
- [ ] 풀이 기록 저장

---

## STEP 9. 학습 분석

### API

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| GET | `/api/analysis/me` | 내 종합 분석 | STUDENT |
| GET | `/api/analysis/me/courses/{cId}` | 강의별 분석 | STUDENT |
| GET | `/api/analysis/courses/{cId}/students` | 학생별 현황 | INSTRUCTOR |
| GET | `/api/analysis/courses/{cId}/overview` | 강의 전체 분석 | INSTRUCTOR |

### 수강생 분석 응답 예시

```json
{
  "overallScore": 78,
  "totalStudyTime": 4200,
  "quizStats": { "totalAttempts": 25, "correctRate": 0.72 },
  "weakConcepts": [
    { "concept": "벡터 임베딩", "correctRate": 0.4 },
    { "concept": "어텐션 메커니즘", "correctRate": 0.5 }
  ],
  "recommendations": [
    "벡터 임베딩 복습이 필요합니다. Lecture 3을 다시 학습해보세요."
  ],
  "weeklyProgress": [
    { "week": "2026-W14", "studyTime": 1200, "quizScore": 65 }
  ]
}
```

### 교강사 대시보드 응답 예시

```json
{
  "totalStudents": 35,
  "averageScore": 72,
  "completionRate": 0.85,
  "topWeakConcepts": [
    { "concept": "벡터 임베딩", "avgCorrectRate": 0.45 }
  ],
  "frequentQuestions": [
    { "question": "임베딩 차원은 어떻게 정하나요?", "count": 12 }
  ]
}
```

### 체크리스트

- [ ] 학습 로그 자동 수집
- [ ] 수강생 개인 분석
- [ ] 취약 개념 도출 로직
- [ ] AI 학습 추천 생성
- [ ] 교강사 대시보드 데이터

---

## STEP 10. Docker 배포 + 통합 테스트

### docker-compose.yml

```yaml
version: '3.8'
services:
  backend:
    build: ./Memora_Server
    ports: ["8080:8080"]
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - AI_SERVER_URL=http://ai-server:8000
    depends_on: [postgres, ai-server]

  ai-server:
    build: ./memora-ai
    ports: ["8000:8000"]
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - CALLBACK_URL=http://backend:8080
    volumes:
      - vector_data:/app/vector_store

  postgres:
    image: postgres:16
    ports: ["5432:5432"]
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

### Dockerfile (Spring Boot)

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/Memora_Server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Dockerfile (FastAPI)

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### AWS 배포 (향후)

```
EC2 (t3.medium) — Docker Compose 구동
RDS (PostgreSQL) — 프로덕션 DB
S3 — 파일 저장
Route 53 — 도메인
```

### 체크리스트

- [ ] Dockerfile 작성 (Spring Boot, FastAPI)
- [ ] docker-compose.yml
- [ ] .env 관리
- [ ] 로컬 Docker 실행 테스트
- [ ] 전체 API 통합 테스트
- [ ] AWS 배포 (선택)

---

## 프롬프트 설계

### 1. 문서 요약

```
당신은 교육 콘텐츠 전문가입니다.
아래 강의 자료를 분석하여 구조화된 요약을 생성하세요.

[문서 내용]
{document_text}

형식:
1. **핵심 개요** (2-3문장)
2. **주요 개념** (bullet point)
3. **핵심 키워드** (쉼표 구분)
4. **학습 목표**
```

### 2. 질의응답 (RAG)

```
당신은 친절한 학습 도우미입니다.
아래 강의 자료를 참고하여 학생의 질문에 답변하세요.

[참고 자료]
{retrieved_chunks}

[이전 대화]
{chat_history}

[학생 질문]
{question}

[난이도: {difficulty}]

규칙:
- 참고 자료 기반으로 정확하게 답변
- 자료에 없으면 "제공된 자료에는 해당 내용이 없습니다" 안내
- 난이도에 맞는 수준으로 설명 (EASY=비유 중심, MEDIUM=정확한 설명, HARD=심화)
- 출처 명시
```

### 3. 문제 생성

```
당신은 교육 평가 전문가입니다.
강의 내용 기반으로 학습 확인용 문제를 생성하세요.

[강의 내용]
{lecture_content}

요청: {count}개 / {quiz_types} / 난이도 {difficulty}

JSON 형식으로 출력:
{ "question", "quizType", "options", "correctAnswer", "explanation", "conceptTag" }

규칙: 이해 확인 문제, 그럴듯한 오답 포함, 해설 포함
```

### 4. 채점/피드백

```
당신은 교육 전문 채점관입니다.

[문제] {question}
[정답] {correct_answer}
[학생 답안] {user_answer}
[유형: {quiz_type}]

JSON 출력: { "isCorrect", "score", "feedback", "improvement" }

규칙: 서술형은 키워드+논리 평가, 부분 점수 허용, 틀린 이유 설명
```

### 5. 학습 분석

```
당신은 학습 데이터 분석 전문가입니다.

[학습 데이터]
정답률: {correct_rate} / 취약 개념: {weak_concepts}
질문 패턴: {question_patterns} / 학습시간 추이: {study_time_trend}

분석 항목:
1. 현재 학습 상태 진단
2. 취약 개념 분석
3. 맞춤형 학습 추천 3가지
4. 동기 부여 메시지
```

---

## Spring Boot 패키지 구조

```
src/main/java/com/kit/memora_server/
├── MemoraServerApplication.java
├── global/
│   ├── config/          # SecurityConfig, WebClientConfig, S3Config, SwaggerConfig, CorsConfig
│   ├── common/          # ApiResponse, PageResponse, ErrorResponse
│   ├── exception/       # GlobalExceptionHandler, BusinessException, ErrorCode
│   └── security/        # JwtTokenProvider, JwtAuthFilter, CustomUserDetails
├── domain/
│   ├── auth/            # controller, dto, service
│   ├── user/            # entity, repository, enums
│   ├── course/          # controller, dto, entity, repository, service
│   ├── lecture/         # controller, dto, entity, repository, service
│   ├── document/        # controller, dto, entity, repository, service (S3Service 포함)
│   ├── qa/              # controller, dto, entity, repository, service
│   ├── quiz/            # controller, dto, entity, repository, service
│   └── analysis/        # controller, dto, entity, repository, service
└── infra/
    └── ai/              # AiServerClient (WebClient), dto, AiCallbackController
```

---

## MVP 범위 정리

### 반드시 포함 (P0)
- 회원가입/로그인 (JWT)
- 강의 생성/조회
- PDF 업로드 → AI 문서 처리
- 문서 요약 조회
- AI 질의응답 (RAG)
- 객관식 문제 생성 + 채점

### 가능하면 포함 (P1)
- 서술형 문제 + AI 채점
- 학습 분석 대시보드
- 학습 추천
- 교강사 학생 현황

### 제외 (MVP 이후)
- OAuth 소셜 로그인
- 실시간 알림 (WebSocket)
- 협업 기능
- 외부 LMS 연동
- 결제 시스템
