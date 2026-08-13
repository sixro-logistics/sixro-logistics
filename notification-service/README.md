# 🔔 Notification Service

알림(Notification) 처리를 담당하는 서비스로, Slack 메시지 발송, AI 기반 메시지 생성 및 Kafka 이벤트를 통한 비동기 알림 시스템을 제공합니다.

## 📌 기본 정보

| 항목 | 내용 |
| --- | --- |
| 서비스명 | `notification-service` |
| 포트 | 별도 설정 포트 (예: `19096` 등) |
| 데이터베이스 | PostgreSQL |
| 메시지 브로커 | Kafka (`delivery.events` 토픽) |
| 캐시/멱등성 | Redis |
| 서비스 통신 | Slack API (OpenFeign) |

## ✨ 주요 기능

- Slack 메시지 CRUD 및 발송 (Admin API)
- 배송 생성 이벤트(`DeliveryCreatedEvent`) 자동 소비 및 알림 발송
- AI 모델을 활용한 메시지 템플릿 자동 생성
- Kafka Transactional Producer/Consumer 패턴 구현
- Redis 기반 이벤트 멱등성(Idempotency) 보장
- 장애 대비 DLQ(Dead Letter Queue) 전략 적용

## 🌐 Slack API (Admin 권한 전용)

`MASTER_ADMIN` 권한을 가진 사용자만 접근 가능합니다.

| Method | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/slack/messages` | 메시지 목록 조회 (검색 조건 지원) |
| `GET` | `/api/v1/slack/messages/{slackMessageId}` | 메시지 상세 조회 |
| `PATCH` | `/api/v1/slack/messages/{slackMessageId}` | 메시지 내용 수정 |
| `DELETE` | `/api/v1/slack/messages/{slackMessageId}` | 메시지 삭제 |

* `POST /api/v1/slack/messages`: 일반 발송 (인증된 사용자 전용)

## 📨 이벤트 파이프라인

### 1. Producer (배송 생성 이벤트)
- **트랜잭션 연동**: `@TransactionalEventListener(phase = AFTER_COMMIT)`를 사용하여 DB 커밋 직후에만 Kafka로 이벤트를 발행합니다.
- **헤더 구성**: `event-type` 및 `trace-id`를 포함하여 분산 추적 가능.

### 2. Consumer (알림 처리)
- **멱등성 처리**: Redis `setIfAbsent`를 사용하여 동일한 `eventId`를 7일간 1회만 처리하도록 보장합니다.
- **로직**:
    1. AI 서비스(`AiService`) 호출 → 메시지 템플릿 생성.
    2. 배정된 배송 담당자(`DeliveryManagerInfo`) 확인 → 각각의 Slack ID로 발송.
    3. 담당자 미배정 시 → 관리자 채널(`adminSlackId`)로 알림 발송.

## ⚙️ Kafka 설정 및 장애 복구 (KafkaConfig)

- **에러 핸들러**: `DefaultErrorHandler`를 통해 재시도 로직 구성.
- **재시도 정책**: 1초 간격으로 최대 3회 재시도.
- **DLQ (Dead Letter Queue)**: 재시도 실패 시 `delivery.events.dlq` 토픽으로 메시지를 전달하여 데이터 유실 방지.

---

[⬅️ 프로젝트 메인 문서로 이동](../README.md)
