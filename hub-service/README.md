# 🏬 Hub Service

**허브**와 **허브간 경로** 정보를 관리합니다.

`Delivery Service` 와 통신하여 최적 허브 경로 탐색을 제공하고, 허브별 물동량 및 운영 상태를 관리합니다.

## 📌 기본 정보

| 항목 | 내용                         |
| --- |----------------------------|
| 서비스명 | `hub-service`              |
| 포트 | `19098`                    |
| 데이터베이스 | PostgreSQL, PostGIS, Redis |
| 스키마 | hub_schema                 |
| 동기 통신 | RestClient                 |
| 비동기 통신 | Kafka                      |
| 서비스 디스커버리 | Eureka                     |
| API 문서 | Springdoc OpenAPI, Swagger UI |
| 분산 추적 | Micrometer,Zipkin          |

## ✨ 주요 기능

- Hub, HubRoute CRUD API

- Hub, HubROute 목록 조회 및 페이징·검색

- Redis 캐싱을 통한 조회 성능 개선
  - `look-aside` - Hub, HubRoute, RouteNetwork, HubClosed
  - `write-behind` - Volume

- API-Gateway JWT 기반 인증 처리
- Header 의 역할/소속 기반 인가 처리

- 허브 물동량 및 HubStatus 관리
  - HubStatus 전이 규칙 적용
  - Delivery 이벤트 기반 Volume 증감
  - Utilization 수치에 따라 HubStatus 전환 및 이벤트 발행

- Dijkstra 기반 경로 탐색 기능
  - 탐색 옵션 - `Optimal`, `Distance`, `Durateion`, `Cost`
  - cost 정책 반영
  - 허브별 혼잡률에 따른 변동 환적시간 반영

## 🌐 API 목록

| Method | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/hub-routes` | 신규 허브 생성 |
| `GET` | `/api/v1/hub-routes/{hubId}` | 허브 상세 조회 |
| `GET` | `/api/v1/hub-routes` | 허브 목록 조회 (페이징, 검색, 정렬) |
| `PUT` | `/api/v1/hub-routes/{hubId}` | 허브 정보 수정 |
| `DELETE` | `/api/v1/hub-routes//{hubId}` | 허브 정보 삭제 |
| `PATCH` | `/api/v1/hub-routes/{hubId}/status` | 허브 상태 변경 |
| `GET` | `/api/v1/hub-routes/{hubId}` | 최인접 허브 조회 |
|  |  |  |
| `POST` | `/api/v1/hub-routes` | 신규 경로 생성 |
| `GET` | `/api/v1/hub-routes/{hubRouteId}` | 경로 상세 조회 |
| `GET` | `/api/v1/hub-routes` | 경로 목록 조회 (페이징, 검색, 정렬) |
| `PUT` | `/api/v1/hub-routes/{hubRouteId}` | 경로 정보 수정 |
| `DELETE` | `/api/v1/hub-routes//{hubRouteId}` | 경로 정보 삭제 |
| `POST` | `/api/v1/hub-routes/{hubRouteId}/sync` | TMAP API 기반 전체 경로 생성 (초기화) |
| `PATCH` | `/api/v1/hub-routes/{hubRouteId}/sync` | TMAP API 기반 단건 경로 갱신 |
| `POST` | `/api/v1/hub-routes/sync-all` | TMAP API 기반 전체 경로 갱신 |
|  |  |  |
| `GET` | `/api/v1/internal/hubs/{hubId}` | 운영 허브 조회 |
| `GET` | `/api/v1/internal/hub-routes/paths` | 허브간 최적 경로 탐색 |
|  |  |  |
| `POST` | `/api/v1/internal//mock/hubs/volume` | 배송 이벤트 발행 트리거 |

## 🗃️ ERD

![erd cloud 이미지](./docs/images/6로배송%20erd.png)

## 🛠️ 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Persistence | Spring Data JPA, Hibernate |
| Dynamic Query | QueryDSL 5.1 |
| DB | PostgreSQL, PostGIS, Redis |
| Messaging | Kafka |
| Cloud | Spring Cloud 2025.0.3 |
| Service Discovery | Netflix Eureka |
| Build | Gradle |
| API Documentation | Springdoc OpenAPI, Swagger UI |
| Tracing | Micrometer Tracing, Brave, Zipkin |
| Validation | Jakarta Bean Validation |
| Test | JUnit 5, Mockito, AssertJ, H2, Testcontainers |

---

[⬅️ 프로젝트 메인 문서로 이동](../README.md)