# Inventory Service README

Sixro Logistics의 상품 재고를 관리하는 서비스입니다.

주문 생성 과정에서는 Order Service의 동기 요청을 통해 재고를 차감하고,
주문 취소 및 배송 생성 실패와 같은 후속 처리에서는 Kafka 이벤트를 소비하여 재고를 복원합니다.

## ✨ 주요 기능

- 재고 생성
- 재고 조회
- 재고 수정
- 재고 삭제
- 재고 목록 조회
- 주문 생성에 따른 재고 차감 API 제공
- 주문 생성 실패에 따른 재고 복원 API 제공
- 주문 취소에 따른 재고 복원
- 주문 실패에 따른 재고 복원
- `order.canceled` 이벤트 소비
- `order.failed` 이벤트 소비
- OpenFeign을 통한 서비스 간 동기 통신 지원
- Kafka 기반 이벤트 통신
- 이벤트 소비 멱등성 보장
- DLT(Dead Letter Topic) 처리

## 🔄 주요 이벤트 및 통신 흐름

### 주문 생성에 따른 재고 차감

주문 생성 시 Order Service에서 Inventory Service의 재고 차감 API를 동기적으로 호출합니다.

```text
Order Service
      │
      │ OpenFeign
      │ 재고 차감 요청
      ▼
Inventory Service
      │
      ▼
재고 조회
      │
      ▼
재고 차감
      │
      ▼
차감 결과 반환
      │
      ▼
Order Service
```

재고가 충분하지 않거나 재고 정보를 찾을 수 없는 경우
재고 차감에 실패하고 주문 생성이 진행되지 않도록 처리합니다.

### 주문 취소에 따른 재고 복원

주문이 취소되면 Order Service에서 `order.canceled` 이벤트를 발행합니다.

```text
Order Service
      │
      │ order.canceled
      ▼
Kafka
      │
      ▼
Inventory Service
      │
      ▼
재고 복원
```

Inventory Service는 주문에 포함된 상품 및 수량 정보를 기준으로
차감되었던 재고를 다시 복원합니다.

### 배송 생성 실패에 따른 재고 복원

배송 생성에 실패하면 Order Service에서 `order.failed` 이벤트를 발행합니다.

```text
Delivery Service
      │
      │ delivery.creation.failed
      ▼
Order Service
      │
      │ order.failed
      ▼
Kafka
      │
      ▼
Inventory Service
      │
      ▼
재고 복원
```

이를 통해 배송 생성에 실패한 주문의 재고를 다시 복원할 수 있습니다.

## 📡 서비스 간 통신

### 동기 통신

주문 생성 과정에서 재고 차감은 OpenFeign을 이용한 동기 통신으로 처리합니다.

```text
Order Service ── OpenFeign ──> Inventory Service
                              │
                              └── 재고 차감
```

재고 차감 결과를 확인한 후 주문 생성을 계속 진행하기 때문에
재고가 부족한 주문이 생성되는 것을 방지할 수 있습니다.

### 비동기 이벤트 통신

주문 취소 및 배송 생성 실패에 따른 재고 복원은 Kafka 이벤트를 통해 비동기적으로 처리합니다.

```text
order.canceled ──┐
                 ├──> Inventory Service ──> 재고 복원
order.failed ────┘
```

## 📡 Kafka 이벤트

### 소비 이벤트

| 이벤트 | 설명 |
|---|---|
| `order.canceled` | 주문 취소에 따른 재고 복원 |
| `order.failed` | 배송 생성 실패에 따른 재고 복원 |

Inventory Service는 위 이벤트를 Kafka Consumer를 통해 비동기적으로 처리합니다.

## 🔁 이벤트 멱등성

Kafka 이벤트는 네트워크 및 Consumer 재처리 과정에서
동일한 이벤트가 중복 전달될 수 있습니다.

이를 방지하기 위해 이벤트 ID를 기준으로 이미 처리된 이벤트인지 확인하고,
동일한 이벤트가 다시 전달되더라도 재고가 중복으로 복원되지 않도록 처리합니다.

```text
Kafka Event
    │
    ▼
이벤트 처리 여부 확인
    │
    ├── 이미 처리됨 → 처리하지 않음
    │
    └── 미처리
          ↓
       재고 복원
          ↓
      처리 이력 저장
```

## 🚨 DLT 처리

이벤트 소비 및 비즈니스 처리 과정에서 반복적으로 실패한 이벤트는
Dead Letter Topic(DLT)을 통해 별도로 관리합니다.

이를 통해 정상적인 이벤트 처리와 실패한 이벤트를 분리하고,
장애 발생 시 원인 분석 및 재처리가 가능하도록 구성합니다.

## 📦 주요 패키지

```text
com.sixro.logistics.inventory
├── application
│   ├── command
│   ├── event
│   ├── facade
│   └── service
├── domain
│   ├── entity
│   │   ├── inventory
│   │   └── processedEvent
│   └── repository
├── infrastructure
│   ├── kafka
│   └── persistence
└── presentation
    └── controller
```

### 주요 역할

- `application`
  - 재고 Command 및 Application Service
  - Kafka 이벤트 처리
  - 재고 비즈니스 로직

- `domain`
  - 재고 Entity
  - ProcessedEvent Entity
  - 재고 및 이벤트 처리 관련 Repository 인터페이스

- `infrastructure`
  - Kafka Consumer
  - Kafka Topic 관리
  - 재고 및 ProcessedEvent Repository 구현

- `presentation`
  - 재고 REST API Controller
  - 재고 차감 및 복원 API 제공

## 🗄️ 재고 관리

재고는 허브와 상품을 기준으로 관리합니다.

재고 차감 시 Order Service의 동기 요청을 통해 전달받은
`hubId`, `productId`, `quantity`를 기준으로 해당 재고를 조회하고 주문 수량만큼 차감합니다.

재고 복원 시 `order.canceled` 또는 `order.failed` 이벤트에 포함된
상품 및 수량 정보를 기준으로 차감된 재고를 다시 복원시킵니다.

## 🗑️ Soft Delete

재고 데이터는 실제 행을 삭제하지 않고 Soft Delete 방식으로 관리합니다.

삭제 시 공통 `BaseEntity`의 삭제 관련 필드를 사용하여
논리적으로 삭제된 재고와 활성 재고를 구분합니다.

재고 조회 시 삭제된 데이터가 조회되지 않도록 처리합니다.

## 🧪 테스트

재고 서비스의 주요 비즈니스 로직 및 이벤트 처리에 대한 단위 테스트를 작성하고 실행합니다.

프로젝트 루트에서 다음 명령어를 실행합니다.

```bash
./gradlew :inventory-service:test
```

Windows 환경에서는:

```bash
.\gradlew.bat :inventory-service:test
```

---

[⬅️ 프로젝트 메인 문서로 이동](https://github.com/sixro-logistics/sixro-logistics/blob/develop/README.md)
