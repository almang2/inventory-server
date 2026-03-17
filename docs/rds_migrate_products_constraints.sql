-- 기존 RDS 업그레이드용: products 제약/인덱스 추가 스크립트
-- 대상: 이미 products 테이블이 존재하는 환경
--
-- 주의:
-- 1) 아래 스크립트는 한 번만 실행하세요.
-- 2) UNIQUE 제약 추가 전에 중복 데이터가 있으면 실패합니다.

-- 0) 사전 점검: 동일 (store_id, code) 중복 확인
SELECT store_id, code, COUNT(*) AS duplicate_count
FROM products
GROUP BY store_id, code
HAVING COUNT(*) > 1;

-- 1) 중복 데이터 정리 후 실행: 복합 UNIQUE 제약 추가
ALTER TABLE products
    ADD CONSTRAINT uk_products_store_code UNIQUE (store_id, code);

-- 2) 배치 조회 성능 보강 인덱스 추가
CREATE INDEX idx_products_store_id ON products (store_id);
CREATE INDEX idx_products_vendor_id ON products (vendor_id);
CREATE INDEX idx_products_code ON products (code);
CREATE INDEX idx_products_deleted_at ON products (deleted_at);

-- 참고:
-- soft delete(deleted_at) 사용 시 UNIQUE(store_id, code)는 삭제된 행도 포함해 충돌할 수 있습니다.
-- 코드 재사용 정책이 필요하면 DB 특성에 맞는 대안을 검토하세요.
-- - MySQL: generated column(활성 플래그) + 복합 unique
-- - PostgreSQL: partial unique index (WHERE deleted_at IS NULL)
