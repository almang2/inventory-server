-- Retail upload local fixture seed for H2 (application-local.yml)
-- Purpose: create many product codes + inventories so retail upload performance can be compared.
--
-- Usage:
-- 1) Start app with local profile (H2 in-memory)
-- 2) Open H2 console: /h2-console
-- 3) Run this script
--
-- Note:
-- - This script is self-contained for local benchmark.
-- - It creates fixture store/user when missing.
-- - Fixture login:
--     username: fixture_admin
--     password: test1234!

-- 0) Ensure there is at least one store.
INSERT INTO stores (name, is_activate, created_at, updated_at)
SELECT
    'FIXTURE_STORE',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM stores);

-- 1) Ensure fixture user exists (BCrypt for password: test1234!)
INSERT INTO users (store_id, username, password, name, role, deleted_at, created_at, updated_at)
SELECT
    (SELECT s.store_id FROM stores s ORDER BY s.store_id LIMIT 1),
    'fixture_admin',
    '$2b$10$HTtNyJq3SVugM7nVav8V5uJYWN/6yL3L9Vz2Osfl6D99PnH/qMmw6',
    'Fixture Admin',
    'ADMIN',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.username = 'fixture_admin'
);

-- 2) Ensure there is at least one vendor in target store.
INSERT INTO vendors (
    store_id, name, channel, phone_number, email, web_page, order_method, note, is_activate, deleted_at, created_at, updated_at
)
SELECT
    (
        SELECT COALESCE(
            (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
            (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
            (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
        )
    ) AS target_store_id,
    'FIXTURE_VENDOR',
    'WEB',
    '010-0000-0000',
    'fixture@local.test',
    'https://local.test',
    'WEB',
    'seed for retail upload benchmark',
    TRUE,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (SELECT 1) t
WHERE NOT EXISTS (
    SELECT 1
    FROM vendors v
    WHERE v.store_id = (
            SELECT COALESCE(
                (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
                (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
                (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
            )
        )
      AND v.name = 'FIXTURE_VENDOR'
      AND v.deleted_at IS NULL
  );

-- 3) Insert fixture products (if missing)
-- Creates P-0001 ~ P-2000 in current user's store.
-- You can make a large Excel using these codes for upload benchmark.
INSERT INTO products (
    store_id, vendor_id, name, code, unit, is_activate, cost_price, retail_price, wholesale_price, deleted_at, created_at, updated_at
)
SELECT
    (
        SELECT COALESCE(
            (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
            (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
            (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
        )
    ) AS target_store_id,
    COALESCE(
        (SELECT v1.vendor_id FROM vendors v1 WHERE v1.store_id = (
            SELECT COALESCE(
                (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
                (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
                (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
            )
        ) AND v1.name = 'FIXTURE_VENDOR' AND v1.deleted_at IS NULL ORDER BY v1.vendor_id LIMIT 1),
        (SELECT v2.vendor_id FROM vendors v2 WHERE v2.store_id = (
            SELECT COALESCE(
                (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
                (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
                (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
            )
        ) AND v2.deleted_at IS NULL ORDER BY v2.vendor_id LIMIT 1)
    ) AS vendor_id,
    'FIXTURE_PRODUCT_' || RIGHT('0000' || CAST(seq.n AS VARCHAR), 4) AS name,
    'P-' || RIGHT('0000' || CAST(seq.n AS VARCHAR), 4) AS code,
    'EA',
    TRUE,
    5000 + MOD(seq.n, 1000),
    10000 + MOD(seq.n, 2000),
    8000 + MOD(seq.n, 1500),
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (SELECT 1) t
JOIN (
    SELECT x AS n FROM SYSTEM_RANGE(1, 2000)
) seq
WHERE NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.store_id = (
            SELECT COALESCE(
                (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
                (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
                (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
            )
        )
        AND p.code = 'P-' || RIGHT('0000' || CAST(seq.n AS VARCHAR), 4)
        AND p.deleted_at IS NULL
  );

-- 4) Ensure each fixture product has inventory
INSERT INTO inventories (
    product_id, display_stock, warehouse_stock, outgoing_reserved, incoming_reserved, reorder_trigger_point, deleted_at, created_at, updated_at
)
SELECT
    p.product_id,
    100000.000,
    100000.000,
    0.000,
    0.000,
    100.000,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM products p
WHERE p.code LIKE 'P-%'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM inventories i
      WHERE i.product_id = p.product_id
  );

-- 5) Quick check
SELECT COUNT(*) AS fixture_user_count
FROM users
WHERE username = 'fixture_admin';

SELECT COUNT(*) AS fixture_product_count
FROM products p
WHERE p.code LIKE 'P-%'
  AND p.deleted_at IS NULL;

SELECT COUNT(*) AS fixture_inventory_count
FROM inventories i
JOIN products p ON p.product_id = i.product_id
WHERE p.code LIKE 'P-%'
  AND p.deleted_at IS NULL;

SELECT p.code, p.name, i.display_stock
FROM products p
JOIN inventories i ON i.product_id = p.product_id
WHERE p.code LIKE 'P-%'
ORDER BY p.code
LIMIT 20;
