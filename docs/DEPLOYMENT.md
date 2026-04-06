# Memora 배포 가이드 (시연용 - 1주일)

> 시연 환경 가정:
> - **백엔드 (Spring Boot)** : 내 노트북에서 실행
> - **AI 서버 (FastAPI)** : 내 노트북에서 실행
> - **PostgreSQL** : 내 노트북 Docker 컨테이너
> - **프론트엔드 (Next.js)** : Vercel 에 공개 배포
>
> 핵심 챌린지: 공개된 프론트엔드가 노트북의 백엔드를 호출해야 하므로,
> 노트북의 8080 포트를 인터넷에 안전하게 노출시킬 **터널** 이 필요합니다.

---

## 0. 전체 그림

```
[ 사용자 브라우저 ]
        │   https
        ▼
[ Vercel : memora-frontend ]   ← Next.js 정적/SSR 호스팅
        │   https + JWT
        ▼
[ Cloudflare Tunnel / ngrok ]  ← 노트북 8080 을 인터넷에 노출
        │
        ▼
[ 노트북: Spring Boot :8080 ] ──── HTTP ──── [ 노트북: FastAPI :8000 ]
        │
        ▼
[ 노트북: Postgres (Docker) :5432 ]
```

---

## 1. 백엔드 - 운영 모드 준비

### 1-1. `.env` 작성 (시연용 강력한 시크릿)

```bash
cd Memora_Server
cp .env.example .env
```

`.env` 에서 반드시 교체할 값:

| 변수 | 값 예시 |
|---|---|
| `JWT_SECRET` | 256bit 이상 무작위 (예: `openssl rand -hex 64`) |
| `DB_PASSWORD` | 강력한 비밀번호 |
| `CORS_ALLOWED_ORIGINS` | `https://*.vercel.app,https://memora-frontend.vercel.app` (배포 후 정확한 도메인으로 좁히기 권장) |

### 1-2. PostgreSQL 띄우기

```bash
cd ..
docker compose -f docker-compose-dev.yml up -d
```

### 1-3. 백엔드 실행 (운영 프로파일)

```bash
cd Memora_Server
./gradlew bootRun --args='--spring.profiles.active=prod'
```

운영 프로파일 효과:
- `ddl-auto: validate` (스키마 변경 차단)
- 로그 WARN 이상만 출력
- Swagger 기본 비활성화 (필요시 `SWAGGER_ENABLED=true` 로 켜기)
- 에러 응답에서 stacktrace 숨김

### 1-4. 동작 확인

```bash
curl http://localhost:8080/api/courses
```

---

## 2. AI 서버 - 운영 모드 준비

```bash
cd memora-ai
cp .env.example .env   # OPENAI_API_KEY 설정 필수
source venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8000
```

> 노트북 외부에 노출할 필요는 없습니다. 백엔드 → AI 서버 호출은 동일 노트북 내부에서만 발생.

---

## 3. 백엔드를 인터넷에 노출 - 터널 설정

### 옵션 A. Cloudflare Tunnel (추천)

**장점:** 무료 / 영구 URL / HTTPS 자동 / 안정적

```bash
# 1. cloudflared 설치
brew install cloudflared

# 2. 빠른 임시 터널 (인증 불필요, URL 매번 바뀜)
cloudflared tunnel --url http://localhost:8080
# → 출력에서 https://xxxxx.trycloudflare.com 확인

# 3. (선택) 영구 도메인 - Cloudflare 계정 필요
cloudflared tunnel login
cloudflared tunnel create memora-api
cloudflared tunnel route dns memora-api memora-api.your-domain.com
cloudflared tunnel run memora-api
```

### 옵션 B. ngrok

**장점:** 가장 간단

```bash
# 1. 설치
brew install ngrok

# 2. 가입 후 토큰 등록 (한 번만)
ngrok config add-authtoken <your-token>

# 3. 실행
ngrok http 8080
# → 출력에서 https://abcd-1234.ngrok-free.app 확인
```

> ⚠️ 무료 ngrok 은 매번 URL 이 바뀝니다. 시연 직전에 한 번만 띄우고 그 URL 을 프론트 env 에 반영하세요.

### 옵션 C. Tailscale Funnel

이미 Tailscale 사용 중이면:
```bash
tailscale funnel 8080
```

---

## 4. CORS - 백엔드에 프론트 도메인 등록

프론트엔드가 Vercel 에 배포되면 다음과 같은 도메인이 생깁니다:
- `https://memora-frontend.vercel.app` (프로덕션)
- `https://memora-frontend-git-{branch}-{user}.vercel.app` (브랜치 프리뷰)
- `https://memora-frontend-{hash}.vercel.app` (deploy 프리뷰)

`.env` 의 `CORS_ALLOWED_ORIGINS` 를 와일드카드로 잡으면 한 번에 처리:

```env
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://*.vercel.app,https://memora-frontend.vercel.app
```

수정 후 백엔드 재시작.

---

## 5. 프론트엔드 - Vercel 배포

### 5-1. 환경변수 파일

```bash
cd memora-frontend
cp .env.local.example .env.local         # 로컬 개발 (백엔드 = localhost:8080)
```

운영용 `.env.production` 은 만들 필요 없음 — Vercel 대시보드에 직접 등록합니다.

### 5-2. 코드에서 사용 (예시)

`src/lib/api/client.ts` (이미 만들었다면 생략):

```ts
import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  withCredentials: true,
});
```

`NEXT_PUBLIC_` 접두어가 붙어야 브라우저로 노출됩니다.

### 5-3. GitHub 푸시

```bash
cd memora-frontend
git remote add origin https://github.com/<you>/memora-frontend.git
git push -u origin main
```

### 5-4. Vercel 연결

1. https://vercel.com → **New Project** → GitHub 레포 import
2. **Framework Preset**: Next.js (자동 감지)
3. **Environment Variables** 추가:

   | Name | Value |
   |---|---|
   | `NEXT_PUBLIC_API_BASE_URL` | `https://xxxxx.trycloudflare.com` (3번 단계 터널 URL) |

4. **Deploy** 클릭 → 1~2분 대기

배포 완료 후 발급된 도메인 (`https://xxxxx.vercel.app`) 을 백엔드 `CORS_ALLOWED_ORIGINS` 에 추가하고 백엔드 재시작.

---

## 6. 시연 당일 체크리스트

다음 순서로 켜야 합니다:

```bash
# [터미널 1] PostgreSQL
cd Memora && docker compose -f docker-compose-dev.yml up -d

# [터미널 2] AI 서버
cd memora-ai && source venv/bin/activate && uvicorn main:app --port 8000

# [터미널 3] 백엔드
cd Memora_Server && ./gradlew bootRun --args='--spring.profiles.active=prod'

# [터미널 4] 터널
cloudflared tunnel --url http://localhost:8080
# → 발급된 https URL 확인
```

발급된 터널 URL 이 **이전과 다르면**:
1. Vercel → Settings → Environment Variables 에서 `NEXT_PUBLIC_API_BASE_URL` 업데이트
2. **Redeploy** (Vercel 대시보드 → Deployments → 최신 → Redeploy)

> 💡 Cloudflare Tunnel 영구 URL 또는 ngrok 유료 reserved domain 을 쓰면 매번 재배포 안 해도 됩니다.

---

## 7. 자주 발생하는 이슈

| 증상 | 원인 | 해결 |
|---|---|---|
| 브라우저 콘솔 `CORS error` | `CORS_ALLOWED_ORIGINS` 에 프론트 도메인 누락 | `.env` 수정 후 백엔드 재시작 |
| `Mixed content blocked` | 프론트는 https, 백엔드는 http | 반드시 https 터널 사용 |
| `401 Unauthorized` 무한 | JWT 만료 / Refresh 미구현 | 토큰 재발급 로직 확인 |
| `Connection refused (5432)` | Postgres 컨테이너 미기동 | `docker ps` 확인 |
| Vercel 빌드는 성공하지만 API 호출 실패 | env 변수 미반영 | Settings 등록 후 **반드시 Redeploy** |
| 노트북 슬립 → 터널 끊김 | macOS 절전 | `caffeinate -i ./gradlew bootRun` 으로 슬립 방지 |

---

## 8. 시연 끝나고 정리

```bash
# 터널/서버 종료 (Ctrl+C 4개 터미널)
docker compose -f docker-compose-dev.yml down       # 데이터 유지
docker compose -f docker-compose-dev.yml down -v    # 데이터까지 삭제
```

Vercel 프로젝트는 그대로 둬도 무료 한도 내. 백엔드 터널만 끊으면 외부에서 호출 불가능해집니다.

---

## 부록 A. 진짜 운영 환경으로 갈 때

이 가이드는 "시연용" 입니다. 실제 운영 시 추가로 고려할 것들:

- **백엔드 호스팅**: AWS EC2 / Lightsail / Render / Fly.io
- **DB**: AWS RDS / Supabase / Neon
- **AI 서버**: 동일 호스트 또는 GPU 인스턴스 (작은 모델이면 CPU OK)
- **시크릿 관리**: AWS Secrets Manager, Doppler, Vercel/Railway env
- **HTTPS 인증서**: Caddy / Cloudflare 자동
- **도메인**: Cloudflare Registrar / Route53
- **로깅/모니터링**: Sentry, Datadog, CloudWatch
- **DB 마이그레이션**: Flyway / Liquibase 도입 (현재 `ddl-auto: validate` 만 활성)
- **CI/CD**: GitHub Actions → 도커 이미지 빌드 → 호스트 자동 배포
