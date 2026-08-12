# 🌐 Gateway Service

Sixro Logistics의 단일 진입점으로, 외부 요청을 각 마이크로서비스에 전달하고 공통 인증·인가 및 요청 보안 정책을 적용합니다.

## 📌 기본 정보

| 항목 | 내용 |
| --- | --- |
| 서비스명 | `gateway-service` |
| 포트 | `19091` |
| 서비스 탐색 | Netflix Eureka |
| 인증 방식 | RS256 JWT |
| 인증 상태 저장소 | Redis |
| 통신 방식 | Spring Cloud Gateway WebFlux |

## ✨ 주요 기능

- Eureka 기반 `lb://` 서비스 라우팅
- RS256 JWT 서명 및 Issuer 검증
- Access Token Type 검증
- Redis Session 및 로그아웃 Token 검증
- URL·HTTP Method·Role 기반 인가
- 내부 사용자 Header 위조 방지
- 인증 API Rate Limiting
- Request ID 생성 및 전달
- Gateway 요청·응답 로깅
- Micrometer Tracing 및 Zipkin 연동

## 🛣️ 서비스 라우팅

| 요청 경로 | 대상 서비스 |
| --- | --- |
| `/api/v1/auth/**` | Auth Service |
| `POST /api/v1/users` | Auth Service |
| `/api/v1/users/**` | User Service |
| `/api/v1/hubs/**`, `/api/v1/hub-routes/**` | Hub Service |
| `/api/v1/companies/**` | Company Service |
| `/api/v1/products/**` | Product Service |
| `/api/v1/orders/**` | Order Service |
| `/api/v1/inventories/**` | Inventory Service |
| `/api/v1/deliveries/**` | Delivery Service |
| `/api/v1/delivery-routes/**` | Delivery Service |
| `/api/v1/delivery-managers/**` | Delivery Service |
| `/api/v1/slack/messages/**`, `/api/v1/ai/**` | Notification Service |

`POST /api/v1/users` 요청은 비밀번호 암호화를 위해 User Service가 아닌 Auth Service로 전달합니다.

```text
/api/v1/internal/**
```

## 🔐 인증 및 Session 검증

Gateway는 Auth Service가 발급한 JWT를 다음 순서로 검증합니다.

1. RSA 공개키로 JWT 서명을 검증합니다.
2. Issuer가 `auth-service`인지 확인합니다.
3. `tokenType` Claim이 `ACCESS`인지 확인합니다.
4. JWT의 `jti`가 Redis Blacklist에 등록됐는지 확인합니다.
5. JWT의 `sessionId`와 Redis의 현재 Session ID를 비교합니다.
6. URL과 HTTP Method에 필요한 역할을 확인합니다.

사용하는 Redis Key 형식은 다음과 같습니다.

```text
session:{userId}
blacklist:{jti}
```

Redis 장애로 인증 상태를 확인할 수 없는 경우에는 Fail Closed 정책에 따라 요청을 허용하지 않고 `503 Service Unavailable`을 반환합니다.

## 🛡️ 역할 기반 인가

Gateway의 `SecurityConfig`에서 API 경로와 HTTP Method별 1차 역할 검증을 수행합니다.

사용자 역할은 다음과 같습니다.

- `MASTER_ADMIN`
- `HUB_ADMIN`
- `DELIVERY_MANAGER`
- `COMPANY_MANAGER`

본인 여부, 동일 소속 여부, 리소스 소유권과 같은 세부 권한은 실제 도메인 정보를 가진 각 하위 서비스에서 다시 검증합니다.

## 📨 내부 사용자 Header

인증에 성공하면 JWT Claim을 다음 Header로 변환하여 하위 서비스에 전달합니다.

```text
X-User-Id
X-User-Name
X-User-Role
X-Affiliation-Id
X-Affiliation-Type
```

클라이언트가 위 Header를 임의로 전달하더라도 Gateway가 기존 값을 제거하고 검증된 JWT Claim으로 다시 설정합니다.

인증되지 않은 요청에서도 외부에서 전달된 내부 사용자 Header를 제거합니다.

## 🚦 Rate Limiting

인증 전에 반복 호출될 수 있는 다음 API에 IP 기반 Rate Limiting을 적용합니다.

```text
POST /api/v1/auth/login
POST /api/v1/auth/signup
POST /api/v1/auth/reissue
```

현재 설정은 다음과 같습니다.

| 설정 | 값 |
| --- | ---: |
| 초당 충전량 | 2 |
| 최대 버스트 | 5 |
| 요청당 Token | 1 |
| 제한 기준 | 요청 IP |

제한을 초과하면 `429 Too Many Requests`를 반환합니다.

## 🆔 요청 추적 및 로깅

Gateway는 클라이언트가 전달한 `X-Request-Id`를 제거하고 요청마다 새로운 UUID를 생성합니다.

생성된 Request ID는 다음 위치에 전달됩니다.

- 하위 서비스 요청 Header
- Gateway 응답 Header
- Gateway 요청·응답 로그

Gateway 로그에는 다음 정보가 포함됩니다.

- Request ID
- Trace ID
- HTTP Method
- 요청 경로
- 응답 상태
- 처리 시간

Authorization Header와 요청·응답 Body는 로그에 기록하지 않습니다.

## 🔍 분산 추적

Micrometer Tracing과 Zipkin을 이용해 서비스 간 요청 흐름을 추적합니다.

```text
Client → Gateway → Auth Service → User Service → 다른 Serivce 등
```

Zipkin 기본 주소는 다음과 같습니다.

```text
http://localhost:9411
```

---

[⬅️ 프로젝트 메인 문서로 이동](../README.md)