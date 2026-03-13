# Retail 로컬 Fixture 가이드

로컬에서 `소매 엑셀 업로드` 성능 리팩토링 전/후를 비교할 때 바로 쓸 수 있는 최소 절차입니다.

## 1) 로컬 서버 실행

`application-local.yml` 기준으로 실행합니다.

```bash
./gradlew bootRun
```

로컬 측정 시에는 SQL 로그로 인한 오버헤드를 줄이기 위해 `show-sql`을 꺼서 실행하는 것을 권장합니다.

```bash
SPRING_JPA_SHOW_SQL=false ./gradlew bootRun
```

## 2) H2 콘솔에서 시드 SQL 실행

1. 브라우저에서 `/h2-console` 접속
2. JDBC URL: `jdbc:h2:mem:testdb`
3. [`docs/fixtures/retail-seed-h2.sql`](/Users/joonkyo/inventory-server/docs/fixtures/retail-seed-h2.sql) 전체 실행

시드 결과로 `P-0001` ~ `P-2000` 코드가 생성됩니다.

## 3) 샘플 엑셀 업로드

업로드 파일:
[`docs/fixtures/retail-upload-sample.xlsx`](/Users/joonkyo/inventory-server/docs/fixtures/retail-upload-sample.xlsx)

컬럼 포맷(파서 기준):
- B열: 상품 코드
- C열: 상품명
- D열: 수량
- E열: 실매출

## 4) 비교 측정 팁

- 리팩토링 전/후 동일 파일로 5회 실행
- 1회 워밍업 제외 후 평균/중앙값 비교
- SQL 개수/flush 횟수는 Hibernate statistics로 함께 확인

## 5) k6로 업로드 성능 측정

### k6 설치 확인

```bash
k6 version
```

없으면(macOS):

```bash
brew install k6
```

### 실행

```bash
TOKEN='<ACCESS_TOKEN>' \
BASE_URL='http://localhost:8080' \
FILE_PATH='/Users/joonkyo/inventory-server/docs/fixtures/retail-upload-bulk-3000.xlsx' \
VUS=1 \
ITERATIONS=6 \
k6 run /Users/joonkyo/inventory-server/scripts/k6-retail-upload.js
```

### 결과에서 볼 항목

- `http_req_duration`
- `retail_upload_duration`
- `checks`, `http_req_failed`

현재 기본 threshold(스크립트 기준):

- `http_req_failed < 1%`
- `http_req_duration p(95) < 120s`
- `retail_upload_duration p(95) < 120s`

### 권장 데이터 크기

- 빠른 확인: `1,000` rows
- 로컬 비교 권장: `3,000` rows
- 최대 부하 확인: `10,000+` rows

현재 기본 벤치 파일:
[`docs/fixtures/retail-upload-bulk-3000.xlsx`](/Users/joonkyo/inventory-server/docs/fixtures/retail-upload-bulk-3000.xlsx)

`ITERATIONS=6`으로 두고 1회 워밍업을 제외한 5회 평균/중앙값 비교를 권장합니다.
