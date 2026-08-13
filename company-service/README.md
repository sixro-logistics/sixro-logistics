# 🏢 Company Service

업체(Company) 정보의 조회, 등록, 수정, 삭제 및 권한별 제어 기능을 제공합니다.

허브(Hub) 관리와 연계하여 업체의 소속을 관리하고, 사용자 역할에 따른 세부 인가(Authorization)를 수행합니다.

## 📌 기본 정보

| 항목 | 내용 |
| --- | --- |
| 서비스명 | `company-service` |
| 포트 | 별도 설정 포트 (예: `19094` 등) |
| 데이터베이스 | PostgreSQL |
| 스키마 | `company_schema` |
| 서비스 통신 | RestTemplate / OpenFeign |

## ✨ 주요 기능

- 업체 목록 조회 및 페이징·검색 (허브 ID, 업체명, 업체 유형)
- 업체 상세 단건 조회
- 신규 업체 등록 (역할별 권한 검증)
- 기존 업체 정보 수정 (역할별 권한 검증)
- 업체 Soft Delete 기반 삭제 (역할별 권한 검증)
- Gateway 및 헤더 기반 인증 정보(`X-User-Role`, `X-Affiliation-Id`) 검증

## 🌐 사용자 및 외부 API

| Method | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/companies` | 업체 목록 조회 (페이징, 검색 조건 지원) |
| `GET` | `/api/v1/companies/{companyId}` | 업체 상세 조회 |
| `POST` | `/api/v1/companies` | 신규 업체 등록 |
| `PATCH` | `/api/v1/companies/{companyId}` | 기존 업체 정보 수정 |
| `DELETE` | `/api/v1/companies/{companyId}` | 기존 업체 삭제 |

## 🌐 내부 API

| Method | 경로                                 | 설명               |
| --- |------------------------------------|------------------|
| `GET` | `/api/v1/internal/companies/check` | 업체 존재 유무 조회(리스트) |
| `GET` | `/api/v1/internal/companies/hub-info` | 목적지 허브를 반환 |

## 🔒 권한 및 인가 정책 (Role-Based Access Control)

각 API 요청 시 전달되는 헤더(`X-User-Role`, `X-Affiliation-Id`)를 바탕으로 권한을 검증합니다.

### 1. 업체 등록 (`POST /api/v1/companies`)
- **`MASTER_ADMIN`**: 소속 정보(`X-Affiliation-Id`)와 관계없이 모든 허브에 업체를 생성할 수 있습니다.
- **`HUB_ADMIN`**: 본인이 담당하는 허브(`affiliationId`)와 등록하려는 요청의 허브 ID(`request.getHubId()`)가 일치할 때만 생성할 수 있습니다.
- 그 외 역할은 접근이 거부(`403 Forbidden`)됩니다.

### 2. 업체 수정 (`PATCH /api/v1/companies/{companyId}`)
- **`MASTER_ADMIN`**: 모든 업체를 수정할 수 있습니다.
- **`HUB_ADMIN`**: 본인이 담당하는 허브와 수정 대상 업체의 허브가 일치할 때만 수정할 수 있습니다.
- **`COMPANY_MANAGER`**: 본인의 소속 업체 ID(`affiliationId` == `companyId`)인 경우에만 수정할 수 있습니다.

### 3. 업체 삭제 (`DELETE /api/v1/companies/{companyId}`)
- **`MASTER_ADMIN`**: 제약 없이 업체를 삭제할 수 있습니다.
- **`HUB_ADMIN`**: 대상 업체의 소속 허브 ID와 관리자의 허브 ID가 일치할 때만 삭제할 수 있습니다.
- 그 외 역할은 접근이 거부(`403 Forbidden`)됩니다.

## 🔎 검색 및 페이징 조건 (`GET /api/v1/companies`)

업체 목록 조회 시 다음과 같은 필터링과 정렬을 지원합니다.

| 파라미터 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `hubId` | UUID | 선택 | 특정 허브에 속한 업체 필터링 |
| `companyName` | String | 선택 | 업체명 검색 |
| `companyType` | String | 선택 | 업체 유형별 필터링 |
| `pageable` | Pageable | 선택 (기본값) | 기본 정렬: `createdAt` 기준 내림차순, 기본 크기: 10건 |

---

[⬅️ 프로젝트 메인 문서로 이동](../README.md)
