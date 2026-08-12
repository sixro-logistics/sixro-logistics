# 👤 User Service

사용자의 가입 상태, 역할, 소속 및 Soft Delete를 관리하고 사용자 변경 Event를 Kafka로 발행합니다.

Hub·Company Service의 내부 API를 호출하여 사용자 소속의 실제 존재 여부도 검증합니다.

## 📌 기본 정보

| 항목 | 내용 |
| --- | --- |
| 서비스명 | `user-service` |
| 포트 | `19093` |
| 데이터베이스 | PostgreSQL |
| 스키마 | `user_schema` |
| 서비스 통신 | OpenFeign |
| Event 발행 | Kafka·Transactional Outbox |

## ✨ 주요 기능

- 사용자 정보 조회·검색·수정
- 가입 승인 및 거절
- 사용자 비활성화
- 역할과 소속 조합 검증
- Hub·Company 소속 존재 여부 검증
- username·Slack ID 중복 검사
- Soft Delete 기반 사용자 관리
- Transactional Outbox Event 발행
- OpenFeign Timeout·Retry·Circuit Breaker

## 🌐 사용자 API

| Method | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/users/me` | 내 정보 조회 |
| `GET` | `/api/v1/users/{userId}` | 사용자 단건 조회 |
| `GET` | `/api/v1/users` | 사용자 목록 조회·검색 |
| `PATCH` | `/api/v1/users/{userId}` | 사용자 정보 수정 |
| `POST` | `/api/v1/users/{userId}/approve` | 가입 승인 |
| `POST` | `/api/v1/users/{userId}/reject` | 가입 거절 |
| `DELETE` | `/api/v1/users/{userId}` | 사용자 비활성화 |

Gateway에서 1차 역할 인가를 수행하고, User Service에서는 본인 여부와 사용자 상태 등 세부 비즈니스 권한을 다시 검증합니다.

## 🔒 내부 API

| Method | 경로 | 사용 목적 |
| --- | --- | --- |
| `POST` | `/api/v1/internal/users` | 회원가입 사용자 생성 |
| `POST` | `/api/v1/internal/users/admin-created` | 승인 상태 사용자 생성 |
| `GET` | `/api/v1/internal/users/auth-info/{username}` | 로그인 인증 정보 조회 |
| `GET` | `/api/v1/internal/users/{userId}/status` | 사용자 최신 상태 조회 |
| `GET` | `/api/v1/internal/users/{userId}/delivery-info` | 배송·수령인 정보 조회 |

`auth-info` 응답에는 비밀번호 해시가 포함되므로 내부 API는 외부에 공개하지 않아야 합니다.

## 👥 사용자 상태

| 상태 | 설명 |
| --- | --- |
| `PENDING` | 가입 승인 대기 |
| `APPROVED` | 로그인과 서비스 이용 가능 |
| `REJECTED` | 가입 거절 |

일반 회원가입 사용자는 `PENDING` 상태로 생성됩니다. 승인과 거절은 `PENDING` 사용자에게만 수행할 수 있습니다.

## 🏢 역할과 소속 정책

| 역할 | 소속 유형 | 소속 ID |
| --- | --- | --- |
| `MASTER_ADMIN` | 없음 | 없음 |
| `HUB_ADMIN` | `HUB` | Hub ID |
| `DELIVERY_MANAGER` | `HUB` | Hub ID |
| `COMPANY_MANAGER` | `COMPANY` | Company ID |

Hub 또는 Company 소속을 저장하기 전에 해당 서비스의 내부 API를 호출하여 실제 존재하는 소속인지 검증합니다.

## 🔗 소속 서비스 연동

```text
GET /api/v1/internal/hubs/{hubId}
GET /api/v1/internal/companies/{companyId}
```

| 설정 | 값 |
| --- | ---: |
| Connect Timeout | 2초 |
| Read Timeout | 3초 |
| 최대 호출 횟수 | 최초 1회 + 재시도 2회 |
| 초기 재시도 간격 | 200ms |
| 최대 재시도 간격 | 1초 |

조회 전용 GET 요청에만 Retry를 적용합니다.

- 모든 5xx 응답은 재시도 대상으로 변환합니다.
- 404·410은 재시도하지 않고 소속 없음으로 처리합니다.
- Hub와 Company 호출에 각각 Circuit Breaker를 적용합니다.
- 장애 발생 시 임의의 소속 정보를 반환하는 Fallback은 사용하지 않습니다.

## 🔎 중복 정책

- username은 삭제된 사용자를 포함하여 중복을 허용하지 않습니다.
- Slack ID는 삭제된 사용자를 포함하여 중복을 검사합니다.
- Slack ID 수정 시 현재 사용자를 제외하고 중복 여부를 확인합니다.

username은 DB Unique 제약과 애플리케이션 중복 검사를 함께 사용합니다.

## 🗑️ Soft Delete

사용자 비활성화는 실제 행을 삭제하지 않고 다음 값을 기록합니다.

```text
isDeleted = true
deletedAt = 비활성화 일시
deletedBy = 처리 사용자 ID
```

활성 사용자 조회 시 다음 조건을 사용합니다.

```sql
is_deleted = false
AND deleted_at IS NULL
AND deleted_by IS NULL
```

삭제된 사용자는 일반 단건·목록·내부 상태 조회 결과에서 제외됩니다.

## 📨 Transactional Outbox

사용자 데이터 변경과 Outbox Event 저장을 동일한 DB Transaction에서 처리합니다.

```text
사용자 변경
    → Outbox Event 저장
    → Polling Publisher 조회
    → Kafka 발행
```

Outbox 상태는 다음과 같습니다.

```text
PENDING → PUBLISHED
        → FAILED
```

- Polling 주기: 1초
- 최대 발행 시도: 3회
- 한 번에 최대 100개 조회
- `FOR UPDATE SKIP LOCKED`로 다중 인스턴스 중복 선점 방지
- 발행 성공을 확인한 뒤 `PUBLISHED`로 변경
- Kafka Header에 `event-id`, `event-type` 포함

## 📬 사용자 Event

| Event | Kafka Topic | 현재 Consumer |
| --- | --- | --- |
| `USER_CREATED` | `user-created` | 추후 확장 예정 |
| `USER_APPROVED` | `user-approved` | 추후 확장 예정 |
| `USER_REJECTED` | `user-rejected` | 추후 확장 예정 |
| `USER_DEACTIVATED` | `user-deactivated` | Auth Service |
| `USER_ROLE_CHANGED` | `user-role-changed` | Auth Service |
| `USER_AFFILIATION_CHANGED` | `user-affiliation-changed` | Auth Service |

Auth Service는 보안 관련 Event를 소비하여 기존 Session과 Refresh Token을 무효화합니다.

---

[⬅️ 프로젝트 메인 문서로 이동](../README.md)