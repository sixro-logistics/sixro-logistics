# 🧭 Discovery Server

Sixro Logistics의 각 마이크로서비스를 등록하고 탐색하기 위한 Eureka Server입니다.

서비스는 Eureka에 자신의 주소와 상태를 등록하며, Gateway와 OpenFeign Client는 서비스 이름을 기준으로 대상 인스턴스를 조회합니다.

## ✨ 주요 기능

- 마이크로서비스 등록
- 서비스 인스턴스 탐색
- 서비스 상태 확인
- Gateway의 `lb://` 라우팅 지원
- OpenFeign의 서비스 이름 기반 호출 지원

## 🌐 기본 정보

| 항목 | 값 |
| --- | --- |
| 애플리케이션 이름 | `discovery-server` |
| 기본 포트 | `19090` |
| Eureka Dashboard | `http://localhost:19090` |
| Eureka Endpoint | `http://localhost:19090/eureka/` |

## 🧩 등록 대상 서비스

- 🌐 Gateway Service
- 🔐 Auth Service
- 👤 User Service
- 🏢 Hub Service
- 🏭 Company Service
- 📦 Product Service
- 🧾 Order Service
- 🗃️ Inventory Service
- 🚛 Delivery Service
- 🔔 Notification Service

## 🔗 서비스 탐색 방식

Gateway는 Eureka에 등록된 서비스 이름을 이용하여 요청을 전달합니다.

```yaml
uri: lb://user-service
```

OpenFeign Client도 같은 방식으로 대상 서비스를 찾습니다.

```java
@FeignClient(name = "user-service")
public interface UserServiceClient {
}
```

서비스의 `spring.application.name`은 Gateway Route 또는 FeignClient의 서비스 이름과 일치해야 합니다.

## 💻 로컬 실행 환경

Windows에서 서비스를 실행하고 WSL 또는 Docker 환경에서 접근하는 경우, Eureka에 접근할 수 없는 hostname이 등록될 수 있습니다.

로컬 통합 테스트에서는 다음과 같이 명시적인 IP 주소를 사용할 수 있습니다.

```yaml
eureka:
  instance:
    prefer-ip-address: ${EUREKA_INSTANCE_PREFER_IP_ADDRESS:true}
    ip-address: ${EUREKA_INSTANCE_IP_ADDRESS:127.0.0.1}
    instance-id: ${spring.application.name}:${server.port}
```

운영 환경에서는 `127.0.0.1`을 고정으로 사용하지 않고 컨테이너 내부 DNS 또는 실제 내부 IP를 환경변수로 전달해야 합니다.

## 🩺 상태 확인

Eureka Dashboard에 접속하여 다음 내용을 확인합니다.

```text
http://localhost:19090
```

확인 항목:

- 서비스 이름
- 등록된 인스턴스 수
- 인스턴스 상태가 `UP`인지 여부
- 등록된 hostname 또는 IP
- 서비스 포트

## ▶️ 실행

프로젝트 루트에서 다음 명령어로 실행합니다.

```powershell
.\gradlew.bat :discovery-server:bootRun
```

다른 서비스가 실행되기 전에 Discovery Server를 먼저 실행하는 것을 권장합니다.

---

[⬅️ 프로젝트 메인 문서로 이동](../README.md)