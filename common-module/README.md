# 🧱 Common Module

Sixro Logistics의 여러 서비스에서 공통으로 사용하는 응답 형식, 예외 처리, 인증 상수 및 JPA 기반 클래스를 제공합니다.

서비스 간 중복 구현을 줄이고 일관된 API 규격과 데이터 관리 정책을 유지하는 것이 목적입니다.

## ✨ 주요 기능

- 공통 API 응답 형식
- 공통 예외 및 오류 응답
- 인증 Header 상수
- JWT Claim 상수
- JPA 감사 필드
- Soft Delete 공통 처리

## 📦 주요 패키지

```text
com.sixro.logistics.common
├── constant
│   ├── HeaderConstants
│   └── JwtClaimConstants
├── core
│   ├── exception
│   └── response
└── persistence
    └── entity
        └── BaseEntity
```

## 📤 공통 API 응답

모든 서비스가 동일한 응답 구조를 사용할 수 있도록 `CommonResponse`를 제공합니다.

```json
{
  "success": true,
  "status": 200,
  "message": "요청을 성공적으로 처리했습니다.",
  "data": {},
  "timestamp": "2026-08-13T12:00:00"
}
```

오류 응답에는 서비스별 오류 코드와 요청 추적을 위한 `requestId`가 포함될 수 있습니다.

```json
{
  "success": false,
  "status": 400,
  "code": "U001",
  "message": "요청을 처리할 수 없습니다.",
  "requestId": "request-id",
  "timestamp": "2026-08-13T12:00:00"
}
```

## 🚨 공통 예외 처리

서비스별 오류 코드는 공통 `ErrorCode` 계약을 구현합니다.

`BaseException`을 통해 다음 정보를 일관된 형태로 전달합니다.

- HTTP 상태 코드
- 서비스 오류 코드
- 클라이언트에 노출할 오류 메시지
- 요청 식별자

각 서비스는 자체 오류 코드를 정의하되 공통 예외 처리 구조를 재사용합니다.

## 🔐 인증 관련 상수

Gateway, Auth 및 각 서비스에서 동일한 Header와 JWT Claim 이름을 사용하도록 공통 상수를 제공합니다.

### 내부 사용자 Header

- `X-User-Id`
- `X-User-Role`
- `X-Affiliation-Id`
- `X-Affiliation-Type`

외부 요청에 포함된 내부 인증 Header는 신뢰하지 않으며, Gateway가 검증한 JWT Claim을 기준으로 덮어써서 전달합니다.

### JWT Claim

- `username`
- `role`
- `affiliationId`
- `affiliationType`
- `sessionId`
- `tokenType`

문자열을 각 서비스에 직접 작성하지 않고 공통 상수를 사용하여 오타와 서비스 간 불일치를 방지합니다.

## 🗄️ BaseEntity

JPA Entity가 공통으로 사용하는 감사 및 Soft Delete 필드를 제공합니다.

| 필드 | 설명 |
| --- | --- |
| `createdAt` | 생성 일시 |
| `createdBy` | 생성 사용자 ID |
| `updatedAt` | 마지막 수정 일시 |
| `updatedBy` | 마지막 수정 사용자 ID |
| `isDeleted` | 논리 삭제 여부 |
| `deletedAt` | 삭제 일시 |
| `deletedBy` | 삭제 처리 사용자 ID |

감사 사용자 ID는 서비스 간 사용자 식별자 형식을 통일하기 위해 `UUID`로 관리합니다.

## 🗑️ Soft Delete

데이터 삭제 시 실제 행을 제거하지 않고 다음 필드를 함께 변경합니다.

```text
isDeleted = true
deletedAt = 현재 시각
deletedBy = 삭제 요청 사용자 ID
```

삭제 처리 사용자 ID가 없거나 이미 삭제된 데이터를 다시 삭제하려는 경우 예외가 발생합니다.

활성 데이터는 다음 조건을 기준으로 조회합니다.

```sql
is_deleted = false
AND deleted_at IS NULL
```

DB에서도 세 필드의 상태가 일치하도록 다음과 같은 제약조건 적용을 권장합니다.

```sql
CHECK (
    (
        is_deleted = false
        AND deleted_at IS NULL
        AND deleted_by IS NULL
    )
    OR
    (
        is_deleted = true
        AND deleted_at IS NOT NULL
        AND deleted_by IS NOT NULL
    )
)
```

## ⚠️ 사용 원칙

- 서비스별 비즈니스 로직은 Common Module에 추가하지 않습니다.
- 여러 서비스가 실제로 공유하는 코드만 포함합니다.
- 서비스별 Enum과 도메인 Entity는 각 서비스에서 관리합니다.
- Common Module 변경은 모든 서비스에 영향을 줄 수 있으므로 전체 빌드로 검증합니다.

## ✅ 검증 명령어

프로젝트 루트에서 다음 명령어를 실행합니다.

```powershell
.\gradlew.bat :common-module:build
```

Common Module 변경이 다른 서비스에 미치는 영향까지 확인하려면 전체 빌드를 실행합니다.

```powershell
.\gradlew.bat build --parallel --build-cache
```

---

[⬅️ 프로젝트 메인 문서로 이동](../README.md)