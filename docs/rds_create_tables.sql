-- 카페24 고객 주문 관련 테이블 생성 SQL
-- 배포 전에 RDS에서 실행해야 합니다.

-- 1. customer_orders 테이블 생성
CREATE TABLE IF NOT EXISTS customer_orders (
    customer_order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cafe24_order_id VARCHAR(50) NOT NULL UNIQUE,
    order_at DATETIME NOT NULL,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    is_canceled BOOLEAN NOT NULL DEFAULT FALSE,
    payment_method VARCHAR(100),
    payment_amount DECIMAL(10, 2) NOT NULL,
    billing_name VARCHAR(100) NOT NULL,
    member_id VARCHAR(100),
    member_email VARCHAR(100),
    initial_order_price_amount DECIMAL(10, 2) NOT NULL,
    shipping_fee DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cafe24_order_id (cafe24_order_id),
    INDEX idx_order_at (order_at),
    INDEX idx_is_paid (is_paid),
    INDEX idx_is_canceled (is_canceled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. products 테이블 생성
-- JPA Product 엔티티와 스키마 정합성을 위해 명시적으로 관리합니다.
CREATE TABLE IF NOT EXISTS products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    name VARCHAR(30) NOT NULL,
    code VARCHAR(30) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    is_activate BOOLEAN NOT NULL DEFAULT TRUE,
    cost_price INT NOT NULL,
    retail_price INT NOT NULL,
    wholesale_price INT NOT NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_store_code UNIQUE (store_id, code),
    FOREIGN KEY (store_id) REFERENCES stores(store_id) ON DELETE RESTRICT,
    FOREIGN KEY (vendor_id) REFERENCES vendors(vendor_id) ON DELETE RESTRICT,
    INDEX idx_products_store_id (store_id),
    INDEX idx_products_vendor_id (vendor_id),
    INDEX idx_products_code (code),
    INDEX idx_products_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NOTE:
-- uk_products_store_code는 soft delete(deleted_at)된 행도 포함해 유니크를 검사합니다.
-- 즉, 동일 (store_id, code)를 재생성하면 충돌할 수 있습니다.
-- 코드 재사용 정책이 필요하면 DB 특성에 맞는 전략을 검토하세요.
-- - MySQL: generated column(활성 플래그) + 복합 unique
-- - PostgreSQL: partial unique index (WHERE deleted_at IS NULL)

-- 3. customer_order_items 테이블 생성
CREATE TABLE IF NOT EXISTS customer_order_items (
    customer_order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    option_value VARCHAR(255),
    variant_code VARCHAR(50),
    item_code VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_order_id) REFERENCES customer_orders(customer_order_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT,
    INDEX idx_customer_order_id (customer_order_id),
    INDEX idx_product_id (product_id),
    INDEX idx_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. retails 테이블 생성 (소매 판매 내역)
CREATE TABLE IF NOT EXISTS retails (
    retail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(30) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sold_date DATE NOT NULL,
    quantity DECIMAL(10, 3) NOT NULL,
    actual_sales INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES stores(store_id) ON DELETE RESTRICT,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT,
    INDEX idx_store_sold_date (store_id, sold_date),
    INDEX idx_sold_date (sold_date),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기존 RDS 업그레이드(이미 존재하는 products 테이블에 제약/인덱스 추가)는
-- 아래 별도 마이그레이션 파일을 사용하세요.
-- docs/rds_migrate_products_constraints.sql
