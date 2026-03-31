# N+1 로컬 측정 메모

`목록 조회 API N+1 리팩토링` 전/후를 같은 조건으로 비교하기 위해 정리한 개인 측정 메모입니다.
아래 명령은 모두 `inventory-server` 리포지토리 루트 경로에서 실행하는 기준으로 적어두었습니다.

## 1) 로컬 서버 실행

`application-local.yml` 기준으로 실행합니다.

```bash
SPRING_JPA_SHOW_SQL=false ./gradlew bootRun
```

쿼리 수와 세션 통계를 함께 보려면 아래처럼 실행합니다.

```bash
SPRING_JPA_SHOW_SQL=false \
SPRING_JPA_PROPERTIES_HIBERNATE_GENERATE_STATISTICS=true \
LOGGING_LEVEL_ORG_HIBERNATE_STAT=DEBUG \
LOGGING_LEVEL_ORG_HIBERNATE_ENGINE_INTERNAL_STATISTICALLOGGINGSESSIONEVENTLISTENER=DEBUG \
./gradlew bootRun
```

SQL 자체를 눈으로 확인할 때만 `show-sql`을 다시 켜고,
응답 시간 측정은 SQL 로그를 끈 상태에서 진행합니다.

## 2) H2 콘솔에서 시드 SQL 실행

1. 브라우저에서 `/h2-console` 접속
2. JDBC URL: `jdbc:h2:mem:testdb`
3. [`n-plus-one-seed-h2.sql`](./n-plus-one-seed-h2.sql) 전체 실행

시드 결과로 아래 fixture 데이터가 생성됩니다.

- fixture 사용자: `fixture_admin`
- fixture 비밀번호: `test1234!`
- fixture 벤더: `FIXTURE_VENDOR_01` ~ `FIXTURE_VENDOR_10`
- fixture 상품: `NP-P-0001` ~ `NP-P-0300`
- fixture 발주: 약 `200`건
- fixture 입고: 약 `150`건

이번 측정에서 사용한 기준은 아래와 같습니다.

- Product 목록: `size=50`
- Inventory 목록: `size=50`
- Order 목록: `size=30`, `size=50`
- Receipt 목록: `size=30`, `size=50`

## 3) 로그인 후 액세스 토큰 준비

```bash
curl -i -X POST 'http://localhost:8080/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "fixture_admin",
    "password": "test1234!"
  }'
```

응답 JSON의 `data.accessToken` 값을 복사해서 아래처럼 사용합니다.

```bash
TOKEN='<ACCESS_TOKEN>'
```

## 4) 대상 API

측정 대상 목록 API는 아래 4개입니다.

- `GET /api/v1/product`
- `GET /api/v1/inventory`
- `GET /api/v1/order`
- `GET /api/v1/receipt`

권장 기본 호출 조건:

- Product: `page=0&size=50`
- Inventory: `page=0&size=50&scope=ALL&sort=updatedAt`
- Order: `page=0&size=30`
- Receipt: `page=0&size=30`

## 5) 수동 측정용 curl 예시

### Product 목록

```bash
curl -s -o /dev/null \
  -w 'product total=%{time_total}\n' \
  'http://localhost:8080/api/v1/product?page=0&size=50' \
  -H "Authorization: Bearer $TOKEN"
```

### Inventory 목록

```bash
curl -s -o /dev/null \
  -w 'inventory total=%{time_total}\n' \
  'http://localhost:8080/api/v1/inventory?page=0&size=50&scope=ALL&sort=updatedAt' \
  -H "Authorization: Bearer $TOKEN"
```

### Order 목록

```bash
curl -s -o /dev/null \
  -w 'order total=%{time_total}\n' \
  'http://localhost:8080/api/v1/order?page=0&size=30' \
  -H "Authorization: Bearer $TOKEN"
```

### Receipt 목록

```bash
curl -s -o /dev/null \
  -w 'receipt total=%{time_total}\n' \
  'http://localhost:8080/api/v1/receipt?page=0&size=30' \
  -H "Authorization: Bearer $TOKEN"
```

반복 측정 기준은 아래와 같습니다.

- 각 API별 6회 호출
- 1회 워밍업 제외
- 나머지 5회의 평균 또는 중앙값 기록

## 6) 쿼리 수 확인 방법

### 방법 A. Hibernate statistics 로그 확인

서버를 아래 옵션으로 실행합니다.

```bash
SPRING_JPA_SHOW_SQL=false \
SPRING_JPA_PROPERTIES_HIBERNATE_GENERATE_STATISTICS=true \
LOGGING_LEVEL_ORG_HIBERNATE_STAT=DEBUG \
LOGGING_LEVEL_ORG_HIBERNATE_ENGINE_INTERNAL_STATISTICALLOGGINGSESSIONEVENTLISTENER=DEBUG \
./gradlew bootRun
```

API 1회 호출 후 로그에서 아래 항목을 확인합니다.

- JDBC statements
- flush 수
- 세션당 실행 통계

이 방법은 전체적인 추이를 보기 좋습니다.

### 방법 B. SQL 로그 직접 확인

짧게 한 번만 볼 때는 SQL 로그를 켜고, API 호출 1회 기준으로 실제 `select` 개수를 셉니다.
특히 아래처럼 page size를 키웠을 때 SQL 수가 함께 증가하면 N+1 의심 근거로 쓰기 좋습니다.

- `size=10`
- `size=30`
- `size=50`

### 방법 C. 로그 파일 기반 반복 집계

콘솔에서 SQL을 눈으로 세기 어려워서, 서버 로그를 파일로 저장한 뒤
측정 구간만 잘라서 `Hibernate:` 개수를 집계하는 방식으로 정리했습니다.

#### 6-1. 서버 로그를 파일로 저장

```bash
./gradlew bootRun | tee /tmp/nplusone.log
```

#### 6-2. 측정 시작 전 현재 줄 수 확인

```bash
wc -l /tmp/nplusone.log
```

예를 들어 결과가 `200 /tmp/nplusone.log` 라면,
이후 측정 구간은 `201`번째 줄부터 보면 됩니다.

#### 6-3. 대상 API 5회 반복 호출

##### Product 목록 `size=50`

```bash
for i in 1 2 3 4 5; do
  curl -s -o /dev/null \
    'http://localhost:8080/api/v1/product?page=0&size=50' \
    -H "Authorization: Bearer $TOKEN"
done
```

##### Inventory 목록 `size=50`

```bash
for i in 1 2 3 4 5; do
  curl -s -o /dev/null \
    'http://localhost:8080/api/v1/inventory?page=0&size=50&scope=ALL&sort=updatedAt' \
    -H "Authorization: Bearer $TOKEN"
done
```

##### Order 목록 `size=10`

```bash
for i in 1 2 3 4 5; do
  curl -s -o /dev/null \
    'http://localhost:8080/api/v1/order?page=0&size=10' \
    -H "Authorization: Bearer $TOKEN"
done
```

##### Order 목록 `size=50`

```bash
for i in 1 2 3 4 5; do
  curl -s -o /dev/null \
    'http://localhost:8080/api/v1/order?page=0&size=50' \
    -H "Authorization: Bearer $TOKEN"
done
```

##### Receipt 목록 `size=10`

```bash
for i in 1 2 3 4 5; do
  curl -s -o /dev/null \
    'http://localhost:8080/api/v1/receipt?page=0&size=10' \
    -H "Authorization: Bearer $TOKEN"
done
```

##### Receipt 목록 `size=50`

```bash
for i in 1 2 3 4 5; do
  curl -s -o /dev/null \
    'http://localhost:8080/api/v1/receipt?page=0&size=50' \
    -H "Authorization: Bearer $TOKEN"
done
```

#### 6-4. 측정 구간 로그만 분리

측정 시작 전 줄 수가 `200`이었다면:

```bash
tail -n +201 /tmp/nplusone.log > /tmp/order-size10.log
```

반복 측정마다 시작 줄 수를 다시 확인해서 파일을 따로 분리하면 됩니다.

예시 파일명:

- `/tmp/product-size50.log`
- `/tmp/inventory-size50.log`
- `/tmp/order-size10.log`
- `/tmp/order-size50.log`
- `/tmp/receipt-size10.log`
- `/tmp/receipt-size50.log`

#### 6-5. 전체 SQL 개수 집계

```bash
rg -c "Hibernate:" /tmp/order-size10.log
```

이 값은 5회 호출 동안 실행된 전체 SQL 개수입니다.
1회 평균 SQL 수는 `전체 SQL 수 / 5` 로 계산합니다.

#### 6-6. 테이블별 반복 쿼리 집계

Order 목록:

```bash
rg -c "from order_items" /tmp/order-size10.log
rg -c "from products" /tmp/order-size10.log
rg -c "count\\(" /tmp/order-size10.log
```

Product 목록:

```bash
rg -c "from products" /tmp/product-size50.log
rg -c "from vendors" /tmp/product-size50.log
rg -c "from stores" /tmp/product-size50.log
rg -c "count\\(" /tmp/product-size50.log
```

Inventory 목록:

```bash
rg -c "from inventories" /tmp/inventory-size50.log
rg -c "from products" /tmp/inventory-size50.log
rg -c "count\\(" /tmp/inventory-size50.log
```

Receipt 목록:

```bash
rg -c "from receipt_items" /tmp/receipt-size10.log
rg -c "from products" /tmp/receipt-size10.log
rg -c "count\\(" /tmp/receipt-size10.log
```

#### 6-7. 기록 예시

- `Product size=50, 5회 총 SQL = ...`
- `Inventory size=50, 5회 총 SQL = ...`
- `Order size=10, 5회 총 SQL = 465`
- `Order size=10, 1회 평균 SQL = 93`
- `Order size=10, order_items 조회 총 50회`
- `Order size=10, products 조회 총 400회`

이런 식으로 적어두면 before / after 비교할 때 다시 보기 편합니다.

## 7) fixture 검증용 SQL

아래 쿼리는 H2 콘솔에서 바로 확인할 수 있습니다.

```sql
SELECT COUNT(*) AS fixture_vendor_count
FROM vendors
WHERE name LIKE 'FIXTURE_VENDOR_%'
  AND deleted_at IS NULL;

SELECT COUNT(*) AS fixture_product_count
FROM products
WHERE code LIKE 'NP-P-%'
  AND deleted_at IS NULL;

SELECT COUNT(*) AS fixture_inventory_count
FROM inventories i
JOIN products p ON p.product_id = i.product_id
WHERE p.code LIKE 'NP-P-%';

SELECT COUNT(*) AS fixture_order_count
FROM orders
WHERE order_message LIKE '[FIXTURE] N+1 order %'
  AND deleted_at IS NULL;

SELECT COUNT(*) AS fixture_order_item_count
FROM order_items oi
JOIN orders o ON o.order_id = oi.order_id
WHERE o.order_message LIKE '[FIXTURE] N+1 order %'
  AND o.deleted_at IS NULL;

SELECT COUNT(*) AS fixture_receipt_count
FROM receipts r
JOIN orders o ON o.order_id = r.order_id
WHERE o.order_message LIKE '[FIXTURE] N+1 order %'
  AND r.deleted_at IS NULL;

SELECT COUNT(*) AS fixture_receipt_item_count
FROM receipt_items ri
JOIN receipts r ON r.receipt_id = ri.receipt_id
JOIN orders o ON o.order_id = r.order_id
WHERE o.order_message LIKE '[FIXTURE] N+1 order %'
  AND r.deleted_at IS NULL;
```

## 8) Before / After 기록 템플릿

필요하면 아래 표 형식으로 before / after를 정리합니다.

| API | 조건 | 개선 전 SQL 수 | 개선 후 SQL 수 | 개선 전 평균 응답(ms) | 개선 후 평균 응답(ms) | 비고 |
|---|---|---:|---:|---:|---:|---|
| Product 목록 | `page=0,size=50` |  |  |  |  | `store`, `vendor` |
| Inventory 목록 | `page=0,size=50` |  |  |  |  | `product` |
| Order 목록 | `page=0,size=30` |  |  |  |  | `items`, `product`, `vendor` |
| Receipt 목록 | `page=0,size=30` |  |  |  |  | `order`, `items`, `product` |

### 8-1. Before 결과 정리 기준

응답 시간은 아래 기준으로 정리했습니다.

- 워밍업 1회 제외
- 같은 조건으로 5회 호출
- 5회 평균 또는 중앙값 사용

SQL 수는 아래 기준으로 정리했습니다.

- 로그 파일 기준 `5회 총 SQL 수` 집계
- 필요하면 `1회 평균 SQL 수 = 총 SQL 수 / 5` 로 함께 적기
정리 방식은 아래와 같습니다.

| API | 조건 | 5회 총 SQL 수 | 1회 평균 SQL 수 | 평균 응답(ms) | 관찰 포인트 |
|---|---|---:|---:|---:|---|
| Product 목록 | `page=0,size=50` |  |  |  | `store`, `vendor` lazy 접근 여부 |
| Inventory 목록 | `page=0,size=50` |  |  |  | `product` lazy 접근 여부 |
| Order 목록 | `page=0,size=10` |  |  |  | `order_items`, `product` 반복 조회 |
| Order 목록 | `page=0,size=50` |  |  |  | page size 증가 시 SQL 증가 |
| Receipt 목록 | `page=0,size=10` |  |  |  | `receipt_items`, `product` 반복 조회 |
| Receipt 목록 | `page=0,size=50` |  |  |  | page size 증가 시 SQL 증가 |

### 8-2. Before 측정 실행 순서

1. 서버를 SQL 로그 저장 모드로 실행합니다.

```bash
./gradlew bootRun | tee /tmp/nplusone.log
```

2. fixture 로그인 후 `TOKEN`을 준비합니다.

3. 워밍업 1회씩 호출합니다.

```bash
curl -s -o /dev/null 'http://localhost:8080/api/v1/product?page=0&size=50' -H "Authorization: Bearer $TOKEN"
curl -s -o /dev/null 'http://localhost:8080/api/v1/inventory?page=0&size=50&scope=ALL&sort=updatedAt' -H "Authorization: Bearer $TOKEN"
curl -s -o /dev/null 'http://localhost:8080/api/v1/order?page=0&size=30' -H "Authorization: Bearer $TOKEN"
curl -s -o /dev/null 'http://localhost:8080/api/v1/receipt?page=0&size=30' -H "Authorization: Bearer $TOKEN"
```

4. 응답 시간 측정은 SQL 로그 없이 따로 측정한 값을 사용합니다.

5. SQL 집계는 대상별로 따로 진행합니다.

- `wc -l /tmp/nplusone.log` 로 시작 줄 수 확인
- 대상 API 5회 호출
- `tail -n +<시작줄수+1> /tmp/nplusone.log > /tmp/<target>.log`
- `rg -c "Hibernate:" /tmp/<target>.log` 로 총 SQL 수 집계

6. `Order`와 `Receipt`는 `size=10`, `size=50` 둘 다 기록합니다.

7. 집계 결과를 위 표에 옮긴 뒤, 리팩토링 후 같은 절차를 다시 반복합니다.

### 8-3. Before 측정 결과 예시

아래는 현재 fixture 기준으로 실제 집계한 개선 전 baseline 결과입니다.

| API | 조건 | 5회 총 SQL 수 | 1회 평균 SQL 수 | 평균 응답(ms) | 관찰 포인트 |
|---|---|---:|---:|---:|---|
| Product 목록 | `page=0,size=50` | 20 | 4.0 | 22.1 | `products 10`, `stores 10`으로 상대적으로 가벼운 편 |
| Inventory 목록 | `page=0,size=50&scope=ALL&sort=updatedAt` | 270 | 54.0 | 17.3 | `products 260` 반복 조회로 `Inventory -> product` N+1 확인 |
| Order 목록 | `page=0,size=10` | 470 | 94.0 | 38.5 | `order_items 50`, `products 400` 반복 조회 |
| Order 목록 | `page=0,size=50` | 1770 | 354.0 | 31.7 | page size 증가에 따라 SQL도 크게 증가 |
| Receipt 목록 | `page=0,size=10` | 470 | 94.0 | 20.3 | `receipt_items 50`, `products 400` 반복 조회 |
| Receipt 목록 | `page=0,size=50` | 1140 | 228.0 | 22.6 | page size 증가에 따라 SQL 증가 패턴 확인 |

응답 시간 평균은 워밍업을 제외한 5회 측정값 기준이고, SQL 수는 로그 파일 집계 기준입니다.
로컬 H2 환경에서는 절대 응답 시간이 빠르게 보일 수 있어서, 이번 리팩토링에서는 `응답 시간 자체`보다
`페이지 크기 증가에 따라 불필요한 SQL이 함께 증가하는 구조`를 더 중요한 문제로 봤습니다.

정리는 아래와 같습니다.

- `Product`는 상대적으로 단순해서 우선순위가 가장 높지는 않았습니다.
- `Inventory`는 목록 50건 조회에서 1회 평균 SQL 54개가 발생해 `Inventory -> product` lazy 접근 비용이 확인됐습니다.
- `Order`는 `size=10`에서 1회 평균 SQL 94개, `size=50`에서 354개로 증가해 가장 강한 N+1 패턴이 보였습니다.
- `Receipt`도 `size=10`에서 94개, `size=50`에서 228개로 증가해 컬렉션 연관 기반 N+1이 확인됐습니다.
- 그래서 리팩토링 우선순위는 `Order`, `Receipt`, `Inventory`, `Product` 순으로 잡았습니다.

## 9) 측정 전 체크

- [ ] Redis 실행 중(`localhost:6379`)
- [ ] 앱 실행 중(`http://localhost:8080`)
- [ ] H2 시드 SQL 실행 완료
- [ ] fixture 로그인 성공 및 액세스 토큰 확보
- [ ] 워밍업 1회 제외 후 측정
- [ ] before / after를 동일 조건으로 비교
