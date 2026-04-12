# Memora 프론트엔드 개발 가이드

> 백엔드 팀 → 프론트엔드 팀 전달 문서
> 최종 수정: 2026-04-06 (교직자 기능 확장)

이 문서는 프론트엔드 개발에 필요한 **모든 API 명세, 데이터 구조, 화면별 연동 가이드**를 포함합니다.
백엔드 API가 완성된 후 이 문서를 기반으로 프론트 작업을 진행하세요.

---

## 목차

1. [기술 스택 및 환경](#1-기술-스택-및-환경)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [API 베이스 정보](#3-api-베이스-정보)
4. [인증 시스템](#4-인증-시스템)
5. [API 명세 전체](#5-api-명세-전체)
6. [화면별 개발 가이드](#6-화면별-개발-가이드)
7. [공통 타입 정의](#7-공통-타입-정의)
8. [상태 관리 설계](#8-상태-관리-설계)
9. [에러 처리 가이드](#9-에러-처리-가이드)

---

## 1. 기술 스택 및 환경

| 항목 | 기술 |
|------|------|
| Framework | Next.js 14+ (App Router) |
| Language | TypeScript |
| 상태관리 | Zustand (클라이언트) + React Query (서버) |
| 스타일 | Tailwind CSS |
| HTTP | Axios 또는 fetch |
| 폼 | React Hook Form + Zod |

### 개발 환경

```
백엔드 API: http://localhost:8080
프론트 개발: http://localhost:3000
Swagger UI: http://localhost:8080/swagger-ui/index.html
```

---

## 2. 프로젝트 구조

```
src/
├── app/                          # Next.js App Router
│   ├── (auth)/                   # 인증 관련 (로그인/회원가입)
│   │   ├── login/page.tsx
│   │   └── signup/page.tsx
│   ├── (main)/                   # 인증 필요한 페이지
│   │   ├── dashboard/page.tsx
│   │   ├── courses/
│   │   │   ├── page.tsx          # 강의 목록
│   │   │   ├── new/page.tsx      # 강의 생성 (교강사)
│   │   │   └── [id]/
│   │   │       ├── page.tsx      # 강의 상세
│   │   │       └── lectures/
│   │   │           └── [lectureId]/
│   │   │               ├── page.tsx      # 학습 페이지
│   │   │               ├── qa/page.tsx   # AI 질문
│   │   │               └── quiz/page.tsx # 문제 풀이
│   │   └── analysis/page.tsx     # 학습 분석
│   └── layout.tsx
├── components/
│   ├── common/                   # 공통 컴포넌트
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Modal.tsx
│   │   ├── Loading.tsx
│   │   └── ErrorBoundary.tsx
│   ├── layout/
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   └── Layout.tsx
│   ├── course/
│   │   ├── CourseCard.tsx
│   │   ├── CourseList.tsx
│   │   └── LectureList.tsx
│   ├── document/
│   │   ├── FileUpload.tsx
│   │   ├── DocumentSummary.tsx
│   │   └── ProcessingStatus.tsx
│   ├── qa/
│   │   ├── ChatWindow.tsx
│   │   ├── ChatMessage.tsx
│   │   ├── ChatInput.tsx
│   │   └── SourceReference.tsx
│   ├── quiz/
│   │   ├── QuizCard.tsx
│   │   ├── MultipleChoice.tsx
│   │   ├── ShortAnswer.tsx
│   │   ├── QuizResult.tsx
│   │   └── QuizGenerator.tsx
│   └── analysis/
│       ├── ScoreChart.tsx
│       ├── WeakConceptList.tsx
│       ├── ProgressTimeline.tsx
│       └── RecommendationCard.tsx
├── lib/
│   ├── api/                      # API 호출 함수
│   │   ├── client.ts             # Axios 인스턴스 (인터셉터)
│   │   ├── auth.ts
│   │   ├── courses.ts
│   │   ├── lectures.ts
│   │   ├── documents.ts
│   │   ├── qa.ts
│   │   ├── quiz.ts
│   │   └── analysis.ts
│   └── utils/
│       ├── token.ts              # JWT 저장/조회
│       └── format.ts             # 날짜/시간 포맷
├── stores/                       # Zustand 스토어
│   ├── authStore.ts
│   └── uiStore.ts
├── hooks/                        # React Query 커스텀 훅
│   ├── useAuth.ts
│   ├── useCourses.ts
│   ├── useQa.ts
│   ├── useQuiz.ts
│   └── useAnalysis.ts
└── types/                        # TypeScript 타입
    ├── api.ts                    # 공통 응답 타입
    ├── auth.ts
    ├── course.ts
    ├── document.ts
    ├── qa.ts
    ├── quiz.ts
    └── analysis.ts
```

---

## 3. API 베이스 정보

### Base URL
```
개발: http://localhost:8080/api
운영: https://api.memora.kr/api
```

### 공통 응답 형식

**성공:**
```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": { ... }
}
```

**실패:**
```json
{
  "success": false,
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "이메일 형식이 올바르지 않습니다.",
  "timestamp": "2026-04-06T14:30:00"
}
```

**페이지네이션:**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### TypeScript 공통 타입

```typescript
// types/api.ts
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface ErrorResponse {
  success: false;
  status: number;
  code: string;
  message: string;
  timestamp: string;
}
```

---

## 4. 인증 시스템

### JWT 토큰 관리

- **accessToken**: 요청 헤더에 포함 (`Authorization: Bearer {token}`)
- **refreshToken**: accessToken 만료 시 갱신에 사용
- 저장 위치: `localStorage` 또는 메모리 (보안 수준에 따라 결정)
- accessToken 만료: 1시간 / refreshToken 만료: 7일

### Axios 인터셉터 예시

```typescript
// lib/api/client.ts
import axios from 'axios';

const client = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
});

// 요청 인터셉터: 토큰 자동 첨부
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터: 401 시 토큰 갱신
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        const { data } = await axios.post('/api/auth/refresh', { refreshToken });
        localStorage.setItem('accessToken', data.data.accessToken);
        error.config.headers.Authorization = `Bearer ${data.data.accessToken}`;
        return client.request(error.config);
      }
      // refreshToken도 만료 → 로그인 페이지로 이동
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;
```

---

## 5. API 명세 전체

### 5-1. 인증 API

#### 회원가입
```
POST /api/auth/signup
Content-Type: application/json
인증: 불필요
```

**Request:**
```json
{
  "email": "student@kit.ac.kr",
  "password": "password123!",
  "name": "김학생",
  "role": "STUDENT"
}
```
- `role`: `"STUDENT"` | `"INSTRUCTOR"`

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "student@kit.ac.kr",
    "name": "김학생",
    "role": "STUDENT",
    "createdAt": "2026-04-06T10:00:00"
  }
}
```

**에러:**
| 상황 | status | code |
|------|--------|------|
| 이메일 중복 | 409 | `DUPLICATE_EMAIL` |
| 유효성 실패 | 400 | `INVALID_INPUT` |

---

#### 로그인
```
POST /api/auth/login
인증: 불필요
```

**Request:**
```json
{ "email": "student@kit.ac.kr", "password": "password123!" }
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "email": "student@kit.ac.kr",
      "name": "김학생",
      "role": "STUDENT"
    }
  }
}
```

**에러:**
| 상황 | status | code |
|------|--------|------|
| 이메일 없음 | 401 | `INVALID_CREDENTIALS` |
| 비밀번호 틀림 | 401 | `INVALID_CREDENTIALS` |

---

#### 토큰 갱신
```
POST /api/auth/refresh
인증: 불필요
```

**Request:**
```json
{ "refreshToken": "eyJ..." }
```

**Response (200):**
```json
{
  "success": true,
  "data": { "accessToken": "eyJ...(새 토큰)", "refreshToken": "eyJ..." }
}
```

---

#### 내 정보 조회
```
GET /api/auth/me
인증: 필요
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "student@kit.ac.kr",
    "name": "김학생",
    "role": "STUDENT",
    "createdAt": "2026-04-06T10:00:00"
  }
}
```

---

### 5-2. 강의 API

#### 강의 목록
```
GET /api/courses?page=0&size=20
인증: 필요
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "인공지능 개론",
        "description": "AI 기초부터 응용까지",
        "instructor": { "id": 5, "name": "이교수" },
        "studentCount": 35,
        "lectureCount": 12,
        "status": "ACTIVE",
        "isEnrolled": true,
        "inviteCode": "A7KQ3M2P",
        "createdAt": "2026-03-01T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

> `inviteCode` 필드는 **소유 강사(INSTRUCTOR)에게만** 포함됩니다. 수강생/비소유 사용자 응답에서는 생략됩니다.

---

#### 강의 생성 (교강사)
```
POST /api/courses
인증: 필요 (INSTRUCTOR)
```

**Request:**
```json
{ "title": "인공지능 개론", "description": "AI 기초부터 응용까지" }
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "인공지능 개론",
    "description": "AI 기초부터 응용까지",
    "instructor": { "id": 5, "name": "이교수" },
    "studentCount": 0,
    "lectureCount": 0,
    "status": "ACTIVE",
    "isEnrolled": false,
    "inviteCode": "A7KQ3M2P",
    "createdAt": "2026-04-06T11:00:00"
  }
}
```

> 강의 생성 시 8자리 초대 코드(A-Z + 2-9, 헷갈리는 I/O/0/1 제외)가 자동 발급됩니다.

---

#### 강의 상세
```
GET /api/courses/{courseId}
인증: 필요
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "인공지능 개론",
    "description": "AI 기초부터 응용까지",
    "instructor": { "id": 5, "name": "이교수" },
    "studentCount": 35,
    "lectureCount": 12,
    "status": "ACTIVE",
    "isEnrolled": true,
    "inviteCode": "A7KQ3M2P",
    "createdAt": "2026-03-01T09:00:00"
  }
}
```

> 차시 목록은 `GET /api/courses/{courseId}/lectures` 를 별도 호출해 조회합니다.

---

#### 수강 등록
```
POST /api/courses/{courseId}/enroll
인증: 필요 (STUDENT)
```

**Response (200):**
```json
{ "success": true, "message": "수강 등록되었습니다." }
```

---

#### 초대 코드로 수강 등록
```
POST /api/courses/enroll-by-code
인증: 필요 (STUDENT)
```

**Request:**
```json
{ "inviteCode": "A7KQ3M2P" }
```

**Response (200):**
```json
{
  "success": true,
  "message": "수강 등록되었습니다.",
  "data": {
    "id": 1,
    "title": "인공지능 개론",
    "description": "AI 기초부터 응용까지",
    "instructor": { "id": 5, "name": "이교수" },
    "studentCount": 36,
    "lectureCount": 12,
    "status": "ACTIVE",
    "isEnrolled": true,
    "createdAt": "2026-03-01T09:00:00"
  }
}
```

- 잘못된 코드: `INVALID_INVITE_CODE` (404)
- 이미 수강 중: `ALREADY_ENROLLED` (409)

---

#### 초대 코드 재발급 (교강사)
```
POST /api/courses/{courseId}/invite-code/regenerate
인증: 필요 (INSTRUCTOR, 소유)
```

**Response (200):**
```json
{
  "success": true,
  "message": "초대 코드가 재발급되었습니다.",
  "data": {
    "id": 1,
    "title": "인공지능 개론",
    "instructor": { "id": 5, "name": "이교수" },
    "studentCount": 35,
    "lectureCount": 12,
    "status": "ACTIVE",
    "isEnrolled": false,
    "inviteCode": "B9XY5N7R",
    "createdAt": "2026-03-01T09:00:00"
  }
}
```

---

### 5-3. 차시/자료 API

#### 차시 생성 (교강사)
```
POST /api/courses/{courseId}/lectures
인증: 필요 (INSTRUCTOR, 소유)
```

**Request:**
```json
{ "title": "1강: AI란 무엇인가", "description": "AI의 정의와 역사" }
```

---

#### PDF 업로드 (교강사)
```
POST /api/lectures/{lectureId}/documents
Content-Type: multipart/form-data
인증: 필요 (INSTRUCTOR)
```

**Request:**
```
file: (PDF 파일, 최대 50MB)
```

**Response (202 Accepted):**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "originalName": "AI개론_1장.pdf",
    "fileType": "PDF",
    "fileSize": 2048576,
    "processingStatus": "PENDING",
    "createdAt": "2026-04-06T11:00:00"
  }
}
```

> `processingStatus`가 `"PENDING"` → `"PROCESSING"` → `"COMPLETED"` 순으로 변경됩니다.
> 프론트에서는 **폴링** 또는 **주기적 조회**로 상태를 확인하세요.

---

#### 자료 목록 조회
```
GET /api/lectures/{lectureId}/documents
인증: 필요 (수강생)
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "originalName": "AI개론_1장.pdf",
      "fileType": "PDF",
      "fileSize": 2048576,
      "processingStatus": "COMPLETED",
      "chunkCount": 24,
      "createdAt": "2026-04-06T11:00:00"
    }
  ]
}
```

---

#### 문서 요약 조회
```
GET /api/documents/{documentId}/summary
인증: 필요 (수강생)
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "originalName": "AI개론_1장.pdf",
    "summary": "## 핵심 개요\nAI는 인간의 지능을 모방하여...\n\n## 주요 개념\n- 인공지능의 정의\n- 튜링 테스트\n- 머신러닝 vs 딥러닝\n\n## 핵심 키워드\nAI, 튜링 테스트, 머신러닝, 딥러닝\n\n## 학습 목표\n이 자료를 통해 AI의 기본 개념과..."
  }
}
```

---

### 5-4. AI 질의응답 API

#### QA 세션 생성
```
POST /api/lectures/{lectureId}/qa/sessions
인증: 필요
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "lectureId": 5,
    "title": "AI개론 1강 질문",
    "messages": [],
    "createdAt": "2026-04-06T14:00:00"
  }
}
```

---

#### 내 QA 세션 목록
```
GET /api/qa/sessions?lectureId=5
인증: 필요
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "lectureId": 5,
      "lectureTitle": "1강: AI란 무엇인가",
      "title": "AI개론 1강 질문",
      "lastMessage": "RAG에서 retrieval은...",
      "messageCount": 6,
      "createdAt": "2026-04-06T14:00:00",
      "updatedAt": "2026-04-06T14:30:00"
    }
  ]
}
```

---

#### 세션 상세 (대화 내역)
```
GET /api/qa/sessions/{sessionId}
인증: 필요 (소유자)
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "lectureId": 5,
    "title": "AI개론 1강 질문",
    "messages": [
      {
        "id": 1,
        "role": "USER",
        "content": "RAG에서 retrieval은 어떤 역할을 하나요?",
        "sources": null,
        "createdAt": "2026-04-06T14:00:00"
      },
      {
        "id": 2,
        "role": "ASSISTANT",
        "content": "RAG(Retrieval-Augmented Generation)에서 Retrieval은 사용자 질의와 관련된 문서를 벡터 데이터베이스에서 검색하는 단계입니다...",
        "sources": [
          {
            "documentId": 10,
            "documentName": "AI개론_3장.pdf",
            "pageNumber": 12,
            "preview": "Retrieval은 사용자의 질의와 관련된 문서를 검색하여 LLM에게 컨텍스트로 제공하는..."
          }
        ],
        "createdAt": "2026-04-06T14:00:05"
      }
    ]
  }
}
```

---

#### 질문 전송 (핵심 API)
```
POST /api/qa/sessions/{sessionId}/messages
인증: 필요 (소유자)
```

**Request:**
```json
{
  "content": "그러면 임베딩은 어떻게 만들어지나요?",
  "difficulty": "MEDIUM"
}
```
- `difficulty`: `"EASY"` | `"MEDIUM"` | `"HARD"` (선택, 기본값 MEDIUM)

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 4,
    "role": "ASSISTANT",
    "content": "임베딩은 텍스트를 고차원 벡터 공간의 수치 벡터로 변환하는 과정입니다...",
    "sources": [
      {
        "documentId": 10,
        "documentName": "AI개론_3장.pdf",
        "pageNumber": 15,
        "preview": "임베딩(Embedding)은 텍스트, 이미지 등의 데이터를..."
      },
      {
        "documentId": 10,
        "documentName": "AI개론_3장.pdf",
        "pageNumber": 16,
        "preview": "OpenAI의 text-embedding 모델은..."
      }
    ],
    "createdAt": "2026-04-06T14:05:00"
  }
}
```

> **주의:** 이 API는 AI 처리 시간(3~10초)이 소요됩니다.
> 프론트에서 로딩 상태를 표시하세요.

---

### 5-5. 문제 풀이 API

#### 문제 자동 생성
```
POST /api/lectures/{lectureId}/quizzes/generate
인증: 필요 (수강생)
```

**Request:**
```json
{
  "count": 5,
  "types": ["MULTIPLE_CHOICE", "SHORT_ANSWER"],
  "difficulty": "MEDIUM"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
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
    },
    {
      "id": 102,
      "question": "임베딩(Embedding)의 역할을 간단히 설명하세요.",
      "quizType": "SHORT_ANSWER",
      "options": null,
      "difficulty": "MEDIUM",
      "conceptTag": "임베딩"
    }
  ]
}
```

> **중요:** 응답에는 `correctAnswer`가 포함되지 않습니다.
> 정답은 제출 후 채점 결과에서 확인합니다.

---

#### 문제 목록 조회
```
GET /api/lectures/{lectureId}/quizzes?difficulty=MEDIUM
인증: 필요 (수강생)
```

---

#### 답안 제출 + 채점
```
POST /api/quizzes/{quizId}/submit
인증: 필요
```

**Request:**
```json
{ "userAnswer": "B", "timeSpent": 30 }
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "attemptId": 501,
    "isCorrect": true,
    "score": 100,
    "correctAnswer": "B",
    "explanation": "RAG의 Retrieval 단계는 사용자 질의와 관련된 문서를 벡터 DB에서 검색하여 LLM에게 컨텍스트로 제공합니다.",
    "aiFeedback": "정확하게 이해하고 있습니다! Retrieval 과정에서 벡터 유사도 검색이 핵심 역할을 합니다.",
    "timeSpent": 30
  }
}
```

---

#### 내 풀이 기록
```
GET /api/lectures/{lectureId}/quizzes/attempts
인증: 필요
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "attemptId": 501,
      "quizId": 101,
      "question": "RAG에서 Retrieval 단계의 주요 목적은?",
      "quizType": "MULTIPLE_CHOICE",
      "userAnswer": "B",
      "correctAnswer": "B",
      "isCorrect": true,
      "score": 100,
      "conceptTag": "RAG",
      "attemptedAt": "2026-04-06T15:00:00"
    }
  ]
}
```

---

#### 문제 수동 생성 (교강사)
```
POST /api/lectures/{lectureId}/quizzes
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Request:**
```json
{
  "question": "RAG에서 Retrieval 단계의 주요 목적은?",
  "quizType": "MULTIPLE_CHOICE",
  "options": [
    "A. 모델을 학습시키기 위해",
    "B. 관련 문서를 검색하기 위해",
    "C. 응답을 생성하기 위해",
    "D. 데이터를 전처리하기 위해"
  ],
  "correctAnswer": "B",
  "explanation": "Retrieval은 관련 문서를 벡터 DB에서 검색합니다.",
  "difficulty": "MEDIUM",
  "conceptTag": "RAG"
}
```

**Response (201):**
```json
{
  "success": true,
  "message": "문제가 생성되었습니다.",
  "data": {
    "id": 201,
    "question": "RAG에서 Retrieval 단계의 주요 목적은?",
    "quizType": "MULTIPLE_CHOICE",
    "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
    "difficulty": "MEDIUM",
    "conceptTag": "RAG"
  }
}
```

---

#### 문제 수정 (교강사)
```
PUT /api/quizzes/{quizId}
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Request:** (모든 필드 선택 — null 이면 기존 값 유지. `options`/`explanation`/`conceptTag`는 null 전달 시 제거됨)
```json
{
  "question": "수정된 문제 본문",
  "quizType": "MULTIPLE_CHOICE",
  "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
  "correctAnswer": "C",
  "explanation": "수정된 해설",
  "difficulty": "HARD",
  "conceptTag": "RAG"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "문제가 수정되었습니다.",
  "data": { "id": 201, "question": "...", "quizType": "MULTIPLE_CHOICE", "options": [...], "difficulty": "HARD", "conceptTag": "RAG" }
}
```

---

#### 문제 삭제 (교강사)
```
DELETE /api/quizzes/{quizId}
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Response (200):**
```json
{ "success": true, "message": "문제가 삭제되었습니다." }
```

- 학생 풀이 기록이 있는 문제는 삭제 불가: `QUIZ_HAS_ATTEMPTS` (409)
- 이 경우 교강사는 `PUT /api/quizzes/{quizId}` 로 수정만 가능합니다.

---

### 5-6. 학습 분석 API

#### 내 종합 분석
```
GET /api/analysis/me
인증: 필요 (STUDENT)
```

**Response:**
```json
{
  "success": true,
  "data": {
    "overallScore": 78,
    "totalStudyTime": 4200,
    "totalQuizAttempts": 25,
    "overallCorrectRate": 0.72,
    "enrolledCourses": 3,
    "weakConcepts": [
      { "concept": "벡터 임베딩", "correctRate": 0.4, "attemptCount": 5 },
      { "concept": "어텐션 메커니즘", "correctRate": 0.5, "attemptCount": 4 }
    ],
    "recommendations": [
      "벡터 임베딩에 대한 복습이 필요합니다. Lecture 3을 다시 학습해보세요.",
      "어텐션 메커니즘 관련 문제를 추가로 풀어보세요."
    ],
    "weeklyProgress": [
      { "week": "2026-W13", "studyTime": 900, "quizScore": 60 },
      { "week": "2026-W14", "studyTime": 1200, "quizScore": 72 },
      { "week": "2026-W15", "studyTime": 1800, "quizScore": 78 }
    ]
  }
}
```

---

#### 강의별 분석
```
GET /api/analysis/me/courses/{courseId}
인증: 필요 (STUDENT)
```

**Response:**
```json
{
  "success": true,
  "data": {
    "courseId": 1,
    "courseTitle": "인공지능 개론",
    "completedLectures": 8,
    "totalLectures": 12,
    "averageQuizScore": 75,
    "totalQuestions": 15,
    "lectureProgress": [
      { "lectureId": 1, "title": "1강: AI란 무엇인가", "quizScore": 90, "questionCount": 3 },
      { "lectureId": 2, "title": "2강: 머신러닝 기초", "quizScore": 65, "questionCount": 5 }
    ],
    "weakConcepts": [
      { "concept": "경사하강법", "correctRate": 0.33 }
    ]
  }
}
```

---

#### 강의 분석 대시보드 (교강사)
```
GET /api/analysis/courses/{courseId}/overview
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Response:**
```json
{
  "success": true,
  "data": {
    "courseId": 1,
    "courseTitle": "인공지능 개론",
    "totalStudents": 35,
    "activeStudents": 28,
    "averageScore": 72,
    "averageCorrectRate": 0.68,
    "averageStudyTime": 3600,
    "totalQuizAttempts": 420,
    "topWeakConcepts": [
      { "concept": "벡터 임베딩", "correctRate": 0.45, "attemptCount": 82 },
      { "concept": "트랜스포머", "correctRate": 0.52, "attemptCount": 61 }
    ],
    "competencies": {
      "개념 이해력": 72,
      "수학적 사고": 70,
      "비판적 추론": 68,
      "암기력": 74,
      "응용력": 65,
      "문제 해결": 69
    },
    "weeklyProgress": [
      { "week": "2026-W11", "studyTime": 8200, "quizScore": 65 },
      { "week": "2026-W12", "studyTime": 9400, "quizScore": 68 },
      { "week": "2026-W13", "studyTime": 10200, "quizScore": 71 },
      { "week": "2026-W14", "studyTime": 11500, "quizScore": 72 }
    ],
    "studentDistribution": {
      "excellent": 5,
      "good": 15,
      "average": 10,
      "needsHelp": 5
    }
  }
}
```

- `activeStudents`: 최근 7일 이내에 퀴즈 시도 또는 학습 로그가 있는 학생 수
- `averageCorrectRate`: 전체 학급 정답률 (0.0 ~ 1.0)
- `averageStudyTime`: 학생 1인당 평균 학습 시간(초)
- `competencies`: 학급 평균 6개 역량 (0~150 스케일, 초기 구현은 학생별 평균)
- `studentDistribution`: `overallScore` + 활동 여부 기반 분류
  - `excellent` ≥ 85, `good` 70~84, `average` 50~69, `needsHelp` < 50 또는 최근 7일 미접속

---

#### 강의 수강생 목록 (교강사)
```
GET /api/analysis/courses/{courseId}/students
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "userId": 1,
      "name": "김학생",
      "email": "student1@kit.ac.kr",
      "averageScore": 78,
      "correctRate": 0.72,
      "totalQuizAttempts": 25,
      "totalStudyTime": 4200,
      "lastActiveAt": "2026-04-06T14:30:00",
      "status": "GOOD"
    },
    {
      "userId": 2,
      "name": "이학생",
      "email": "student2@kit.ac.kr",
      "averageScore": 45,
      "correctRate": 0.41,
      "totalQuizAttempts": 12,
      "totalStudyTime": 900,
      "lastActiveAt": "2026-03-28T10:00:00",
      "status": "NEEDS_HELP"
    }
  ]
}
```

- `status`: `"EXCELLENT"` | `"GOOD"` | `"AVERAGE"` | `"NEEDS_HELP"`
- `lastActiveAt`: 마지막 학습 로그 또는 퀴즈 시도 시각 중 더 최근 값 (없으면 null)

---

#### 수강생 개별 드릴다운 (교강사)
```
GET /api/analysis/courses/{courseId}/students/{userId}
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "userName": "김학생",
    "userEmail": "student1@kit.ac.kr",
    "courseId": 1,
    "courseTitle": "인공지능 개론",
    "overallScore": 78,
    "totalStudyTime": 4200,
    "totalQuizAttempts": 25,
    "overallCorrectRate": 0.72,
    "lastActiveAt": "2026-04-06T14:30:00",
    "status": "GOOD",
    "weakConcepts": [
      { "concept": "벡터 임베딩", "correctRate": 0.4, "attemptCount": 5 }
    ],
    "weeklyProgress": [
      { "week": "2026-W13", "studyTime": 900, "quizScore": 60 },
      { "week": "2026-W14", "studyTime": 1200, "quizScore": 72 }
    ],
    "competencies": {
      "개념 이해력": 78,
      "수학적 사고": 78,
      "비판적 추론": 78,
      "암기력": 78,
      "응용력": 78,
      "문제 해결": 78
    }
  }
}
```

- 이 엔드포인트는 지정된 강의 범위 내의 시도/학습 로그만 집계합니다.
- 학생이 수강 등록되지 않은 경우 `NOT_ENROLLED` (403) 반환.

---

### 5-7. 공지사항 API

#### 공지사항 작성 (교강사)
```
POST /api/courses/{courseId}/notices
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Request:**
```json
{
  "title": "중간고사 일정 안내",
  "content": "중간고사는 4월 20일 오후 2시에 진행됩니다.",
  "pinned": true
}
```

**Response (201):**
```json
{
  "success": true,
  "message": "공지사항이 작성되었습니다.",
  "data": {
    "id": 10,
    "courseId": 1,
    "title": "중간고사 일정 안내",
    "content": "중간고사는 4월 20일 오후 2시에 진행됩니다.",
    "pinned": true,
    "authorId": 5,
    "authorName": "이교수",
    "createdAt": "2026-04-06T11:00:00",
    "updatedAt": "2026-04-06T11:00:00"
  }
}
```

---

#### 공지사항 목록
```
GET /api/courses/{courseId}/notices
인증: 필요 (수강생 또는 소유 강사)
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "courseId": 1,
      "title": "중간고사 일정 안내",
      "content": "중간고사는 4월 20일 오후 2시에 진행됩니다.",
      "pinned": true,
      "authorId": 5,
      "authorName": "이교수",
      "createdAt": "2026-04-06T11:00:00",
      "updatedAt": "2026-04-06T11:00:00"
    }
  ]
}
```

> `pinned=true` 공지가 먼저 정렬되고, 그 안에서 최신순으로 내려옵니다.
> 수강 등록되지 않은 학생이 호출하면 `NOT_ENROLLED` (403).

---

#### 공지사항 수정 (교강사)
```
PUT /api/notices/{noticeId}
인증: 필요 (작성자 INSTRUCTOR)
```

**Request:** 생성과 동일한 스키마
**Response:** 생성과 동일한 스키마

---

#### 공지사항 삭제 (교강사)
```
DELETE /api/notices/{noticeId}
인증: 필요 (작성자 INSTRUCTOR)
```

**Response (200):**
```json
{ "success": true, "message": "공지사항이 삭제되었습니다." }
```

---

### 5-8. 교직자 피드백 API

#### 피드백 작성 (교강사)
```
POST /api/courses/{courseId}/students/{studentId}/feedback
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Request:**
```json
{ "content": "최근 퀴즈 성적이 많이 올랐어요. 계속 이 페이스로 가세요!" }
```

**Response (201):**
```json
{
  "success": true,
  "message": "피드백이 작성되었습니다.",
  "data": {
    "id": 7,
    "courseId": 1,
    "courseTitle": "인공지능 개론",
    "instructorId": 5,
    "instructorName": "이교수",
    "studentId": 1,
    "studentName": "김학생",
    "content": "최근 퀴즈 성적이 많이 올랐어요. 계속 이 페이스로 가세요!",
    "readByStudent": false,
    "createdAt": "2026-04-06T11:30:00"
  }
}
```

- 지정한 학생이 해당 강의에 수강 등록되지 않았으면 `NOT_ENROLLED` (403)

---

#### 학생별 피드백 조회 (교강사)
```
GET /api/courses/{courseId}/students/{studentId}/feedback
인증: 필요 (INSTRUCTOR, 소유 강의)
```

**Response:** `FeedbackResponse` 배열 (최신순)

---

#### 내가 받은 피드백 (학생)
```
GET /api/feedback/me
인증: 필요 (STUDENT)
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 7,
      "courseId": 1,
      "courseTitle": "인공지능 개론",
      "instructorId": 5,
      "instructorName": "이교수",
      "studentId": 1,
      "studentName": "김학생",
      "content": "최근 퀴즈 성적이 많이 올랐어요.",
      "readByStudent": false,
      "createdAt": "2026-04-06T11:30:00"
    }
  ]
}
```

---

#### 피드백 읽음 처리 (학생)
```
PATCH /api/feedback/{feedbackId}/read
인증: 필요 (수신자 본인)
```

**Response (200):**
```json
{
  "success": true,
  "message": "읽음 처리되었습니다.",
  "data": {
    "id": 7,
    "readByStudent": true,
    "createdAt": "2026-04-06T11:30:00"
  }
}
```

- 본인이 수신자가 아니면 `FORBIDDEN` (403)

---

## 6. 화면별 개발 가이드

### 6-1. 로그인/회원가입

| 항목 | 내용 |
|------|------|
| 경로 | `/login`, `/signup` |
| API | `POST /api/auth/login`, `POST /api/auth/signup` |
| 상태 | Zustand `authStore` (user, tokens) |
| 동작 | 로그인 성공 → 토큰 저장 → `/dashboard` 리다이렉트 |
| 유효성 | 이메일 형식, 비밀번호 최소 8자 |
| 역할 선택 | 회원가입 시 STUDENT / INSTRUCTOR 선택 |

---

### 6-2. 대시보드

| 항목 | 내용 |
|------|------|
| 경로 | `/dashboard` |
| API | `GET /api/courses`, `GET /api/analysis/me` |
| 표시 내용 (학생) | 수강 강의 목록, 최근 학습 활동, 취약 개념 요약, 주간 학습 그래프 |
| 표시 내용 (교강사) | 내 강의 목록, 학생 수, 전체 평균 점수, 자주 나오는 질문 |
| 역할 분기 | `user.role`에 따라 학생/교강사 대시보드 분리 |

---

### 6-3. 강의 관리

| 항목 | 내용 |
|------|------|
| 경로 | `/courses`, `/courses/new`, `/courses/{id}` |
| API | CRUD: `GET/POST/PUT/DELETE /api/courses` |
| 학생 | 강의 목록 조회 + 수강 등록 버튼 |
| 교강사 | 강의 생성 + 차시 관리 + PDF 업로드 |

---

### 6-4. 학습 페이지 (핵심)

| 항목 | 내용 |
|------|------|
| 경로 | `/courses/{id}/lectures/{lectureId}` |
| API | `GET /api/lectures/{lId}/documents`, `GET /api/documents/{id}/summary` |
| 레이아웃 | 좌측: 문서 요약 + 자료 목록 / 우측: AI 챗 또는 문제 풀이 탭 |
| 핵심 | 문서 요약을 읽으면서 바로 AI 질문 가능 |

---

### 6-5. AI 질문 UI (채팅)

| 항목 | 내용 |
|------|------|
| 경로 | `/courses/{id}/lectures/{lectureId}/qa` 또는 학습 페이지 내 탭 |
| API | QA 세션 API 전체 |
| UI | 채팅 형태 (메시지 목록 + 입력창) |
| 기능 | 난이도 선택 드롭다운, 출처 참조 클릭 가능, 세션 목록 사이드바 |
| 로딩 | 질문 전송 후 3~10초 대기 → "AI가 답변을 생성 중입니다..." 표시 |
| 출처 표시 | `sources` 배열의 각 항목을 카드/뱃지로 표시, 클릭 시 해당 내용 미리보기 |

---

### 6-6. 문제 풀이

| 항목 | 내용 |
|------|------|
| 경로 | `/courses/{id}/lectures/{lectureId}/quiz` |
| API | 문제 생성 + 제출 API |
| 흐름 | 문제 생성 요청 → 문제 카드 순서대로 표시 → 답안 입력 → 제출 → 결과 표시 |
| 객관식 | 라디오 버튼으로 선택 |
| 주관식 | 텍스트 입력 |
| 결과 화면 | 정답 여부, 해설, AI 피드백을 카드로 표시 |
| 타이머 | 각 문제별 소요 시간 측정 (`timeSpent`로 전송) |

---

### 6-7. 학습 분석 페이지

| 항목 | 내용 |
|------|------|
| 경로 | `/analysis` |
| API | 분석 API |
| 학생 뷰 | 종합 점수, 주간 진도 차트, 취약 개념 리스트, AI 추천 |
| 교강사 뷰 | 학생 분포 차트, 취약 개념 TOP5, 자주 나온 질문, 학생별 현황 테이블 |
| 차트 | recharts 또는 Chart.js 사용 권장 |

---

### 6-8. 교직자 강의 관리

| 항목 | 내용 |
|------|------|
| 경로 | `/courses/{id}/manage` (권장) — 소유 강사 전용 |
| 역할 | `user.role === 'INSTRUCTOR' && course.instructor.id === user.id` 일 때만 노출 |

**탭 구성**

1. **대시보드**
   - API: `GET /api/analysis/courses/{courseId}/overview`
   - 표시: 평균 점수·정답률·학습 시간 카드, 역량 레이더(6축), 주간 진도 라인, 학생 분포 파이, 취약 개념 TOP5
2. **수강생 관리**
   - 목록 API: `GET /api/analysis/courses/{courseId}/students`
   - 학생 클릭 → 드릴다운 모달/페이지: `GET /api/analysis/courses/{courseId}/students/{userId}`
   - status 뱃지로 필터 가능 (EXCELLENT/GOOD/AVERAGE/NEEDS_HELP)
3. **초대 코드**
   - 강의 상세/대시보드에 `inviteCode` 표시 + 복사 버튼
   - 재발급: `POST /api/courses/{courseId}/invite-code/regenerate`
4. **퀴즈 검수**
   - 생성: `POST /api/lectures/{lectureId}/quizzes` (자동 생성 `/generate` 와 구분)
   - 수정: `PUT /api/quizzes/{quizId}`
   - 삭제: `DELETE /api/quizzes/{quizId}` — `QUIZ_HAS_ATTEMPTS` (409) 발생 시 "학생 풀이 기록이 있어 삭제할 수 없습니다. 수정만 가능합니다." 안내
5. **공지사항**
   - 목록/작성/수정/삭제: `/api/courses/{courseId}/notices`, `/api/notices/{id}`
   - `pinned` 토글 UI 제공
6. **피드백**
   - 학생 드릴다운 화면에서 바로 입력 폼 노출
   - 작성: `POST /api/courses/{courseId}/students/{studentId}/feedback`
   - 작성 이력: `GET /api/courses/{courseId}/students/{studentId}/feedback`

> 학생 측: 대시보드에 "내가 받은 피드백" 카드(`GET /api/feedback/me`) + 읽지 않은 개수 뱃지(`readByStudent === false`).
> 공지사항: 강의 상세 페이지 상단에 `pinned=true`를 먼저 보여주고, 학생은 `GET /api/courses/{courseId}/notices` 로 조회.

---

## 7. 공통 타입 정의

```typescript
// types/auth.ts
interface User {
  id: number;
  email: string;
  name: string;
  role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';
  createdAt: string;
}

interface LoginRequest { email: string; password: string; }
interface SignupRequest { email: string; password: string; name: string; role: string; }
interface TokenResponse { accessToken: string; refreshToken: string; user: User; }

// types/course.ts
interface Course {
  id: number;
  title: string;
  description: string;
  instructor: { id: number; name: string };
  studentCount: number;
  lectureCount: number;
  status: 'ACTIVE' | 'ARCHIVED';
  isEnrolled: boolean;
  /** 소유 강사에게만 제공 */
  inviteCode?: string;
  createdAt: string;
}

interface EnrollByCodeRequest { inviteCode: string; }

interface Lecture {
  id: number;
  title: string;
  description: string;
  orderIndex: number;
  documentCount: number;
  hasCompletedDocuments: boolean;
}

// types/document.ts
interface Document {
  id: number;
  originalName: string;
  fileType: string;
  fileSize: number;
  processingStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  chunkCount: number;
  createdAt: string;
}

interface DocumentSummary {
  id: number;
  originalName: string;
  summary: string;
}

// types/qa.ts
interface QaSession {
  id: number;
  lectureId: number;
  lectureTitle?: string;
  title: string;
  lastMessage?: string;
  messageCount: number;
  messages?: QaMessage[];
  createdAt: string;
  updatedAt: string;
}

interface QaMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  sources: Source[] | null;
  createdAt: string;
}

interface Source {
  documentId: number;
  documentName: string;
  pageNumber: number;
  preview: string;
}

interface AskRequest { content: string; difficulty?: 'EASY' | 'MEDIUM' | 'HARD'; }

// types/quiz.ts
interface Quiz {
  id: number;
  question: string;
  quizType: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER' | 'ESSAY';
  options: string[] | null;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  conceptTag: string;
}

interface QuizGenerateRequest {
  count: number;
  types: string[];
  difficulty: string;
}

interface QuizCreateRequest {
  question: string;
  quizType: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER' | 'ESSAY';
  options?: string[];
  correctAnswer: string;
  explanation?: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD';
  conceptTag?: string;
}

interface QuizUpdateRequest {
  question?: string;
  quizType?: string;
  options?: string[];
  correctAnswer?: string;
  explanation?: string;
  difficulty?: string;
  conceptTag?: string;
}

interface QuizSubmitRequest { userAnswer: string; timeSpent: number; }

interface QuizResult {
  attemptId: number;
  isCorrect: boolean;
  score: number;
  correctAnswer: string;
  explanation: string;
  aiFeedback: string;
  timeSpent: number;
}

// types/analysis.ts
interface MyAnalysis {
  overallScore: number;
  totalStudyTime: number;
  totalQuizAttempts: number;
  overallCorrectRate: number;
  enrolledCourses: number;
  weakConcepts: WeakConcept[];
  recommendations: string[];
  weeklyProgress: WeeklyProgress[];
}

interface WeakConcept { concept: string; correctRate: number; attemptCount: number; }
interface WeeklyProgress { week: string; studyTime: number; quizScore: number | null; }

interface CourseOverview {
  courseId: number;
  courseTitle: string;
  totalStudents: number;
  activeStudents: number;
  averageScore: number;
  averageCorrectRate: number;
  averageStudyTime: number;
  totalQuizAttempts: number;
  topWeakConcepts: WeakConcept[];
  competencies: Record<string, number>;
  weeklyProgress: WeeklyProgress[];
  studentDistribution: { excellent: number; good: number; average: number; needsHelp: number };
}

type StudentStatus = 'EXCELLENT' | 'GOOD' | 'AVERAGE' | 'NEEDS_HELP';

interface CourseStudentSummary {
  userId: number;
  name: string;
  email: string;
  averageScore: number;
  correctRate: number;
  totalQuizAttempts: number;
  totalStudyTime: number;
  lastActiveAt: string | null;
  status: StudentStatus;
}

interface CourseStudentDetail {
  userId: number;
  userName: string;
  userEmail: string;
  courseId: number;
  courseTitle: string;
  overallScore: number;
  totalStudyTime: number;
  totalQuizAttempts: number;
  overallCorrectRate: number;
  lastActiveAt: string | null;
  status: StudentStatus;
  weakConcepts: WeakConcept[];
  weeklyProgress: WeeklyProgress[];
  competencies: Record<string, number>;
}

// types/notice.ts
interface Notice {
  id: number;
  courseId: number;
  title: string;
  content: string;
  pinned: boolean;
  authorId: number;
  authorName: string;
  createdAt: string;
  updatedAt: string;
}

interface NoticeRequest { title: string; content: string; pinned?: boolean; }

// types/feedback.ts
interface InstructorFeedback {
  id: number;
  courseId: number;
  courseTitle: string;
  instructorId: number;
  instructorName: string;
  studentId: number;
  studentName: string;
  content: string;
  readByStudent: boolean;
  createdAt: string;
}

interface FeedbackRequest { content: string; }
```

---

## 8. 상태 관리 설계

### Zustand (클라이언트 상태)

```typescript
// stores/authStore.ts — 사용자 인증 상태
interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  login: (data: TokenResponse) => void;
  logout: () => void;
}
```

### React Query (서버 상태)

| 쿼리 키 | API | 사용 위치 |
|---------|-----|----------|
| `['courses']` | `GET /api/courses` | 강의 목록 |
| `['courses', id]` | `GET /api/courses/{id}` | 강의 상세 |
| `['documents', lectureId]` | `GET /api/lectures/{lId}/documents` | 자료 목록 |
| `['document-summary', docId]` | `GET /api/documents/{id}/summary` | 문서 요약 |
| `['qa-sessions']` | `GET /api/qa/sessions` | QA 세션 목록 |
| `['qa-session', sessionId]` | `GET /api/qa/sessions/{sId}` | 세션 대화 |
| `['quizzes', lectureId]` | `GET /api/lectures/{lId}/quizzes` | 문제 목록 |
| `['analysis-me']` | `GET /api/analysis/me` | 내 분석 |

**Mutation 예시:**
| 뮤테이션 | API | 성공 시 |
|---------|-----|--------|
| `sendMessage` | `POST /api/qa/sessions/{sId}/messages` | `['qa-session', sId]` 무효화 |
| `submitQuiz` | `POST /api/quizzes/{qId}/submit` | 결과 화면 표시 |
| `generateQuiz` | `POST /api/lectures/{lId}/quizzes/generate` | 문제 목록으로 이동 |

---

## 9. 에러 처리 가이드

### HTTP 상태 코드별 처리

| 코드 | 의미 | 프론트 동작 |
|------|------|------------|
| 400 | 입력 오류 | 폼 필드에 에러 메시지 표시 |
| 401 | 인증 실패 | 토큰 갱신 시도 → 실패 시 로그인 페이지 |
| 403 | 권한 없음 | "접근 권한이 없습니다" 알림 |
| 404 | 리소스 없음 | "요청한 데이터를 찾을 수 없습니다" |
| 409 | 충돌 (이메일 중복 등) | 해당 필드에 에러 메시지 |
| 500 | 서버 오류 | "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요." |

### 에러 코드 목록

| code | 설명 |
|------|------|
| `INVALID_INPUT` | 유효성 검증 실패 |
| `DUPLICATE_EMAIL` | 이메일 중복 |
| `INVALID_CREDENTIALS` | 로그인 실패 |
| `TOKEN_EXPIRED` | 토큰 만료 |
| `FORBIDDEN` | 권한 없음 |
| `COURSE_NOT_FOUND` | 강의 없음 |
| `LECTURE_NOT_FOUND` | 차시 없음 |
| `DOCUMENT_NOT_FOUND` | 문서 없음 |
| `DOCUMENT_PROCESSING` | 문서 처리 중 (아직 사용 불가) |
| `SESSION_NOT_FOUND` | QA 세션 없음 |
| `QUIZ_NOT_FOUND` | 문제 없음 |
| `NOTICE_NOT_FOUND` | 공지사항 없음 |
| `FEEDBACK_NOT_FOUND` | 피드백 없음 |
| `NOT_ENROLLED` | 수강 등록 안 됨 |
| `ALREADY_ENROLLED` | 이미 수강 중 |
| `INVALID_INVITE_CODE` | 초대 코드가 유효하지 않음 |
| `QUIZ_HAS_ATTEMPTS` | 이미 학생 풀이 기록이 있어 문제 삭제 불가 (수정만 가능) |
| `AI_SERVER_ERROR` | AI 서버 오류 |

---

## 참고 사항

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`에서 모든 API를 직접 테스트 가능
- **문서 처리 폴링**: 업로드 후 `processingStatus`가 `COMPLETED`가 될 때까지 5초 간격 폴링 권장
- **AI 응답 대기**: 질문/문제생성/채점 API는 3~10초 소요될 수 있음, 반드시 로딩 UI 필요
- **역할 분기**: `user.role`에 따라 UI 렌더링을 분기 (학생/교강사 대시보드, 업로드 권한 등)
- **파일 업로드**: `multipart/form-data`로 전송, Axios 사용 시 `Content-Type` 헤더 자동 설정
