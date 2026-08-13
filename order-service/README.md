# Order Service README

Sixro Logistics의 주문 생성, 조회, 수정, 취소 및 삭제를 담당하는 서비스입니다.

주문 생성 과정에서는 Inventory Service와 동기적으로 통신하여 재고를 차감하고,
주문 및 배송 상태 변경 과정에서는 Kafka 기반 이벤트로 다른 서비스와 비동기 통신합니다.
Outbox Pattern을 통해 주문 도메인 데이터 변경과 이벤트 발행의 일관성을 보장합니다.

## ✨ 주요 기능

- 주문 생성
- 주문 조회
- 주문 목록 조회
- 주문 수정
- 주문 취소
- 주문 삭제
- 주문 상태 관리
- `order.created` 이벤트 발행
- `order.canceled` 이벤트 발행
- `delivery.created` 이벤트 소비
- `delivery.creation.failed` 이벤트 소비
- `order.failed` 이벤트 발행
- Kafka 기반 서비스 간 이벤트 통신
- Outbox Pattern을 통한 이벤트 발행
- 이벤트 소비 멱등성 보장
- DLT(Dead Letter Topic) 처리

## 🔄 주요 이벤트 흐름

### 주문 생성

주문 생성 시 주문 정보와 주문 상품을 저장하고,
주문에 포함된 상품의 재고를 Inventory Service와 동기적으로 통신하여 차감합니다.

재고 차감이 정상적으로 완료된 경우 주문을 생성하고,
이후 `OrderCreatedEvent`를 Outbox에 저장하여 `order.created` 이벤트를 발행합니다.

```text
주문 생성 요청
↓
재고 차감 요청
↓
Inventory Service
↓
재고 차감 완료
↓
Order / OrderItem 저장
↓
OrderCreatedEvent 생성
↓
Outbox 저장
↓
OutboxPublisher
↓
Kafka
↓
Delivery Service
```

재고 차감에 실패한 경우 주문 생성이 진행되지 않도록 처리합니다.

`order.created` 이벤트에는 주문 ID, 주문 허브 ID 및 주문 상품 정보 등이 포함됩니다.

Delivery Service는 해당 이벤트를 소비하여 배송 정보와 배송 이동 경로들을 저장하고,
관련 이벤트를 발행합니다.

### 배송 생성 성공

배송 서비스에서 배송 생성이 완료되면 `delivery.created` 이벤트를 발행합니다.

```text
delivery.created
        ↓
Order Service
        ↓
주문 상태를 DELIVERY_CREATED로 변경
```

Order Service는 해당 이벤트를 소비하여 주문 상태를 `DELIVERY_CREATED`로 변경합니다.

### 배송 생성 실패

배송 생성에 실패하면 `delivery.creation.failed` 이벤트를 소비합니다.

```text
delivery.creation.failed
        ↓
Order Service
        ↓
주문 상태를 FAILED로 변경, 해당 주문 Soft Delete
        ↓
ORDER_FAILED Outbox
        ↓
order.failed
        ↓
Inventory Service
        ↓
재고 복원
```

이를 통해 배송 생성에 실패한 주문의 재고를 다시 복원할 수 있습니다.

## 📡 Kafka 이벤트

### 발행 이벤트

| 이벤트 | 설명 |
|---|---|
| `order.created` | 주문 생성 완료 이벤트 |
| `order.canceled` | 주문 취소 이벤트 |
| `order.failed` | 배송 생성 실패에 따른 주문 실패 이벤트 |

### 소비 이벤트

| 이벤트 | 설명 |
|---|---|
| `delivery.created` | 배송 생성 완료 이벤트 |
| `delivery.creation.failed` | 배송 생성 실패 이벤트 |

## 📨 Outbox Pattern

주문 데이터 변경과 Kafka 이벤트 발행 사이의 데이터 불일치 문제를 방지하기 위해 Outbox Pattern을 사용합니다.

주문 생성 또는 상태 변경 시 비즈니스 데이터와 Outbox 데이터를 동일한 트랜잭션에서 저장합니다.

```text
Business Transaction
        │
        ├── Order 저장
        │
        └── Outbox 저장
                │
                ▼
        OutboxPublisher
                │
                ▼
             Kafka
```

Kafka 발행에 실패하더라도 Outbox 데이터가 남아 있기 때문에
다음 발행 주기에서 다시 처리할 수 있습니다.

## 🔁 이벤트 멱등성

Kafka 이벤트는 동일한 이벤트가 중복 전달될 수 있으므로
이벤트 처리 시 중복 처리를 방지하기 위한 멱등성 처리를 적용합니다.

동일한 이벤트가 여러 번 전달되더라도 주문 상태가 중복으로 변경되지 않도록 처리합니다.

## 🚨 DLT 처리

이벤트 소비 과정에서 반복적인 처리 실패가 발생하는 경우
Dead Letter Topic(DLT)을 통해 실패한 이벤트를 별도로 관리할 수 있도록 구성합니다.

이를 통해 정상적인 이벤트 처리 흐름과 실패한 이벤트를 분리하고,
장애 발생 시 원인 분석 및 재처리가 가능하도록 합니다.

## 📦 주요 패키지

```text
com.sixro.logistics.order
├── application
│   ├── command
│   ├── event
│   ├── facade
│   └── service
├── domain
│   ├── entity
│   │   ├── order
│   │   ├── processedEvent
│   │   └── outbox
│   └── event
├── infrastructure
│   ├── client
│   ├── kafka
│   └── persistence
└── presentation
    └── controller
```

### 주요 역할

- `application`
  - 주문 관련 Command 및 Application Service
  - Outbox 이벤트 발행 처리
  - 서비스 간 이벤트 처리

- `domain`
  - 주문 및 주문 상품 Entity
  - ProcessedEvent Entity
  - Outbox Entity
  - 주문 도메인 이벤트

- `infrastructure`
  - `client`
    - OpenFeign Client를 통한 서비스 간 동기 통신
  - `kafka`
    - Kafka Producer / Consumer
    - Kafka Topic 관리
  - `persistence`
    - 주문, Outbox 및 ProcessedEvent Repository 구현

- `presentation`
  - 주문 REST API Controller

## 🗂️ 주문 상태

주문은 이벤트 및 비즈니스 처리 결과에 따라 상태가 변경됩니다.

```text
CREATED
   │
   ├── 배송 생성 성공
   │       ↓
   │   DELIVERY_CREATED
   │
   ├── 배송 생성 실패
   │       ↓
   │    FAILED
   │
   └── 주문 취소
           ↓
        CANCELED
```

주문 수정 및 취소 가능한 범위를 CREATED 상태로 제한하여
이미 처리된 주문의 잘못된 상태 변경을 방지합니다.

## 🧪 테스트

주문 서비스의 주요 비즈니스 로직 및 이벤트 처리에 대한 단위 테스트를 작성하고 실행합니다.

프로젝트 루트에서 다음 명령어를 실행합니다.

```bash
./gradlew :order-service:test
```

Windows 환경에서는:

```bash
.\gradlew.bat :order-service:test
```

[⬅️ 프로젝트 메인 문서로 이동](https://github.com/sixro-logistics/sixro-logistics/blob/develop/README.md)