# 🔐 Auth Service

Sixro Logistics의 사용자 인증과 JWT 발급, Redis 기반 인증 상태 관리를 담당합니다.

사용자 정보는 직접 저장하지 않고 User Service의 내부 API를 통해 조회·생성합니다.

## 📌 기본 정보

| 항목 | 내용 |
| --- | --- |
| 서비스명 | `auth-service` |
| 포트 | `19092` |
| 인증 방식 | RS256 JWT |
| 인증 상태 저장소 | Redis |
| 사용자 연동 | OpenFeign |
| 비동기 메시지 | Kafka |

## ✨ 주요 기능

- 회원가입
- 로그인
- Access Token·Refresh Token 발급
- Token 재발급
- 로그아웃 및 Access Token 차단
- MASTER_ADMIN의 승인 사용자 직접 생성
- BCrypt 비밀번호 암호화
- 사용자 보안 Event 소비
- User Service 조회 Circuit Breaker
- Micrometer Tracing 및 Zipkin 연동

## 🌐 API

| Method | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/signup` | 회원가입 |
| `POST` | `/api/v1/auth/login` | 로그인 |
| `POST` | `/api/v1/auth/reissue` | Token 재발급 |
| `POST` | `/api/v1/auth/logout` | 로그아웃 |
| `POST` | `/api/v1/users` | MASTER_ADMIN 사용자 생성 |

일반 회원가입으로 생성된 사용자는 `PENDING` 상태가 되며, MASTER_ADMIN의 가입 승인 이후 로그인할 수 있습니다.

MASTER_ADMIN 사용자 생성 API는 비밀번호를 암호화한 뒤 User Service에 승인 상태 사용자 생성을 요청합니다.

## 🔑 JWT

Access Token과 Refresh Token은 RSA 개인키로 서명합니다.

| 설정 | 기본값 |
| --- | --- |
| Issuer | `auth-service` |
| Access Token 유효시간 | 30분 |
| Refresh Token 유효시간 | 8시간 |
| 서명 방식 | RS256 |

주요 JWT Claim은 다음과 같습니다.

```text
sub
username
role
affiliationId
affiliationType
sessionId
tokenType
jti
```

한 번의 로그인에서 생성된 Access Token과 Refresh Token은 동일한 `sessionId`를 공유합니다.

## ⚡ Redis 인증 상태

Auth Service는 다음 Redis Key를 사용합니다.

```text
refresh:{userId}
session:{userId}
blacklist:{jti}
```

| Key | 저장 값 |
| --- | --- |
| `refresh:{userId}` | Refresh Token 해시 |
| `session:{userId}` | 현재 로그인 Session ID |
| `blacklist:{jti}` | 로그아웃된 Access Token 정보 |

Refresh Token 원문은 Redis에 저장하지 않고 해시 값만 저장합니다.

새로운 로그인이나 Token 재발급이 발생하면 기존 인증 상태를 교체합니다. 

로그인·재발급·로그아웃 시 Redis Lua Script를 사용하여 관련 Key를 원자적으로 처리합니다.

## 🔄 Token 재발급

Token 재발급 시 다음 항목을 검증합니다.

- Access Token과 Refresh Token 형식
- 각 Token의 `tokenType`
- 두 Token의 사용자 ID 일치 여부
- 두 Token의 Session ID 일치 여부
- Redis에 저장된 현재 Session
- Redis에 저장된 Refresh Token 해시
- User Service의 최신 사용자 상태

검증이 완료되면 새로운 Token Pair와 Session을 발급합니다.

## 🔗 User Service 연동

OpenFeign을 사용하여 User Service의 내부 API를 호출합니다.

```text
POST /api/v1/internal/users
POST /api/v1/internal/users/admin-created
GET  /api/v1/internal/users/auth-info/{username}
GET  /api/v1/internal/users/{userId}/status
```

연결 및 응답 Timeout은 다음과 같습니다.

| 설정 | 값 |
| --- | ---: |
| Connect Timeout | 2초 |
| Read Timeout | 3초 |

로그인과 Token 재발급에 사용하는 조회 API에는 `authUserService` Circuit Breaker를 적용합니다.

- 4xx 응답은 비즈니스 결과로 판단하여 장애율에서 제외합니다.
- 연결 실패, Timeout 및 5xx 응답은 장애로 기록합니다.
- 사용자 생성 POST 요청에는 자동 Retry를 적용하지 않습니다.
- 장애 발생 시 가짜 사용자 정보를 반환하는 Fallback은 사용하지 않습니다.

## 📨 사용자 보안 Event

Auth Service는 다음 Kafka Topic을 소비합니다.

```text
user-deactivated
user-role-changed
user-affiliation-changed
```

Event를 소비하면 해당 사용자의 인증 상태를 무효화합니다.

```text
session:{userId} 삭제
refresh:{userId} 삭제
```

Session이 삭제되면 Gateway가 기존 Access Token을 즉시 차단합니다.

동일 Event가 중복 전달되더라도 Redis Key 삭제 결과가 같으므로 현재 처리 로직은 기본적인 멱등성을 가집니다.

## 🔍 분산 추적

Micrometer Tracing과 Zipkin을 통해 다음 요청 흐름을 추적합니다.

```text
Gateway → Auth Service → User Service
```

Feign Observation을 적용하여 User Service 내부 API 호출도 동일한 Trace에서 확인할 수 있습니다.

---

[⬅️ 프로젝트 메인 문서로 이동](../README.md)
```