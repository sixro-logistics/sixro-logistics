# 🚚 Sixro Logistics

## 📌 프로젝트 소개

Sixro Logistics는 사용자, 인증/인가, 업체, 상품, 주문, 재고, 허브, 배송 및 알림을 각각의 서비스로 분리한 MSA 기반 물류 관리 시스템입니다.

API Gateway를 단일 진입점으로 사용하며, Eureka를 통한 서비스 탐색과 JWT 기반 인증·인가를 적용합니다. 

서비스 간 동기 통신에는 OpenFeign을 사용하고, 사용자 상태 변경과 같은 비동기 처리는 Kafka와 Transactional Outbox Pattern을 통해 전달합니다.

Micrometer Tracing과 Zipkin을 통해 서비스 간 요청 흐름을 추적할 수 있습니다.

## 🛠️ 주요 기술

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Netflix Eureka
- PostgreSQL, PostGIS
- Redis
- Kafka
- Resilience4j
- Micrometer Tracing
- Zipkin
- Docker

## 🧩 서비스 구성
| 서비스 | 주요 역할                                   |                  상세 문서                   |
| --- |-----------------------------------------|:----------------------------------------:|
| Common Module | 공통 응답, 예외, JWT/Header 상수, 감사 기능, 페이징/정렬 |    [상세 보기](./common-module/README.md)    |
| Discovery Server | Eureka 기반 서비스 등록 및 탐색                   |  [상세 보기](./discovery-server/README.md)   |
| Gateway Service | API 라우팅, JWT 검증, 역할 인가, Session 검증      |   [상세 보기](./gateway-service/README.md)   |
| Auth Service | 회원가입, 로그인, JWT 발급·재발급, 로그아웃 및 인증 상태 무효화 |    [상세 보기](./auth-service/README.md)     |
| User Service | 사용자 상태·권한·소속 관리                         |    [상세 보기](./user-service/README.md)     |
| Hub Service | 허브 및 허브 간 이동 경로 관리                      |     [상세 보기](./hub-service/README.md)     |
| Company Service | 생산업체와 수령업체 관리                           |   [상세 보기](./company-service/README.md)    |
| Product Service | 상품 정보 관리                                |   [상세 보기](./product-service/README.md)    |
| Order Service | 주문 생성과 상태 관리                            |    [상세 보기](./order-service/README.md)     |
| Inventory Service | 허브별 상품 재고 관리                            |  [상세 보기](./inventory-service/README.md)   |
| Delivery Service | 배송 및 배송 경로, 배송 담당자 관리                           |   [상세 보기](./delivery-service/README.md)   |
| Notification Service | Slack 메시지 및 AI 기반 알림 처리                 | [상세 보기](./notification-service/README.md) |


## 👥 팀원별 담당 서비스

| 담당 | 서비스 / 개발                                                                               | 기타 역할 |
|---|----------------------------------------------------------------------------------------|---|
| 황태영 | `company-service`, `product-service` , `notification-service`        | `AWS - CD` |
| 정나영 | `order-service`, `inventory-service`                                 | |
| 차은지 | `common-module`, `user-service`, `auth-service`, `gateway-service`, `discovery-server` | |
| 김동현 | `hub-service`                                                        | `GitHub Actions - CI` |
| 손유진 | `delivery-service`                                                   | |


