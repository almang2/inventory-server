# inventory-server

## 🫶 프로젝트 배경: Tech for Impact Campus

본 프로젝트는 **Tech for Impact Campus(https://techforimpact.io/campus/info)** 프로그램에서 **알맹상점**과 협업하여 진행한 프로젝트입니다.

알맹상점은 현재 아날로그 방식으로 발주–입고–재고–판매를 관리하고 있으며 이 과정에서 다음과 같은 문제가 존재했습니다.

- 재고누락, 중복발주 등 **재고 정확도 문제**
- 발주처가 다양하지만 발주처 관리가 전산화되지 않아 **발주 프로세스 비효율**
- 판매/입고 데이터를 체계적으로 축적하지 못해 **데이터 기반 의사결정이 어려움**

이를 해결하기 위해, **발주·입고·재고·판매 전 과정을 디지털화하는 백엔드 시스템**을 구현하였습니다.

---

## 🚀 개요

inventory-server는 알맹상점의 발주·재고·판매 관리를 위한 **Spring Boot 기반 백엔드 서버**입니다.  
상품, 발주처, 발주, 입고, 재고, 판매, 사용자 인증을 통합 관리합니다.

---

## 🌱 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.7 |
| Build | Gradle |
| Database | MySQL (AWS RDS / Local) |
| ORM | Spring Data JPA |
| Cache / Tokens | Redis |
| API Docs | springdoc-openapi (Swagger UI) |
| Auth | JWT(Authentication), Refresh Token(Redis 저장) |
| Infra | AWS EC2, RDS, Route53 |
| 배포 | GitHub Actions CI + 무중단 배포(Blue-Green, Nginx) |

---

## 📁 폴더 구조

```
src
└── main
    ├── java
    │   └── com.almang.inventory
    │       ├── admin          # 관리자 기능
    │       ├── customerorder  # 고객 주문
    │       ├── global         # 공통 설정·보안·예외·유틸
    │       ├── inventory      # 재고(재고 조정·조회)
    │       ├── order          # 발주(발주 요청·상태)
    │       ├── product        # 상품(단위·가격·정보)
    │       ├── receipt        # 입고/출고 전표
    │       ├── retail         # 소매 판매
    │       ├── store          # 매장 정보
    │       ├── user           # 사용자·권한·인증
    │       ├── vendor         # 발주처 도메인
    │       └── wholesale      # 도매 판매
    └── resources
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── data.sql (필요 시)
```

---

## ✨ 주요 기능 요약

### 🔹 상품(Product)
- 상품 등록/수정/삭제
- 가격·단위 등 기본 속성 관리

### 🔹 발주처(Vendor)
- 발주처 관리
- 상품–발주처 매핑

### 🔹 발주(Order)
- 발주 생성/상태 변경
- 발주 요청 → 승인/확정 → 입고 대기

### 🔹 입고/전표(Receipt)
- 입고 처리
- 전표 기반 검수
- 입고 → 재고 반영

### 🔹 재고(Inventory)
- 재고 조회/조정
- 재고 임계치 기반 자동 재발주 추천

### 🔹 판매(Retail/Wholesale)
- 소매·도매 판매 정보 기록
- 판매량 기반 재고 차감

### 🔹 사용자/권한(User)
- 로그인/회원가입
- JWT + Redis 기반 인증 구조

### 🔹 공통(Global)
- API 통합 응답 포맷(ApiResponse)
- 예외처리, 보안, 로깅
- Discord Webhook 에러 알림

---

## ⚙️ 환경 변수 (.env)

현재 프로젝트에서 사용되는 `.env` 파일 구조는 다음과 같습니다.

```bash
# .env

# JWT
JWT_SECRET=
ACCESS_TOKEN_EXPIRATION_MINUTES=

# REFRESH TOKEN
REFRESH_TOKEN_EXPIRATION_DAYS=

# DB
DB_USERNAME=
DB_PASSWORD=
DB_HOST=
DB_NAME=inventory
DB_PORT=3306

# REDIS
REDIS_HOST=localhost
REDIS_PORT=6379

# DISCORD WEBHOOK
DISCORD_WEBHOOK_URL=
```

---

## 🏃 실행 방법

### 1. 클론

```bash
git clone https://github.com/almang2/inventory-server.git
cd inventory-server
```

### 2. 환경 변수 설정

```bash
cp .env.example .env
```

### 3. 서버 실행

```bash
./gradlew bootRun
```

또는 빌드 후 실행

```bash
./gradlew clean build
java -jar build/libs/inventory-server-0.0.1-SNAPSHOT.jar
```

---

## 📘 API 문서

Swagger UI:

```
http://localhost:8080/swagger-ui/index.html
```

---

## ☁️ 배포 구조

본 프로젝트는 **무중단 배포(Blue-Green)** 방식으로 운영됩니다.

- AWS EC2 (Blue/Green systemd 서비스)
- Nginx Reverse Proxy + SSL(Let’s Encrypt)
- GitHub Actions CI (테스트·빌드 자동화)
- AWS RDS(MySQL), Redis
