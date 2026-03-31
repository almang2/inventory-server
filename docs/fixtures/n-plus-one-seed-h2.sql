-- N+1 list query local fixture seed for H2 (application-local.yml)
-- Purpose: create products, inventories, orders, order_items, receipts, receipt_items
-- so Product / Inventory / Order / Receipt list APIs can be compared before and after refactoring.
--
-- Usage:
-- 1) Start app with local profile (H2 in-memory)
-- 2) Open H2 console: /h2-console
-- 3) Run this script
--
-- Fixture login:
--   username: fixture_admin
--   password: test1234!

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

-- 2) Ensure 10 fixture vendors exist.
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
    'FIXTURE_VENDOR_' || RIGHT('00' || CAST(seq.n AS VARCHAR), 2) AS name,
    CASE MOD(seq.n, 3)
        WHEN 0 THEN 'EMAIL'
        WHEN 1 THEN 'MESSAGE'
        ELSE 'WEB'
    END,
    '010-1000-' || RIGHT('0000' || CAST(seq.n AS VARCHAR), 4),
    'fixture-vendor-' || CAST(seq.n AS VARCHAR) || '@local.test',
    'https://fixture-vendor-' || CAST(seq.n AS VARCHAR) || '.local.test',
    CASE MOD(seq.n, 3)
        WHEN 0 THEN 'EMAIL'
        WHEN 1 THEN 'MESSAGE'
        ELSE 'WEB'
    END,
    'seed for N+1 list benchmark',
    TRUE,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT x AS n FROM SYSTEM_RANGE(1, 10)
) seq
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
      AND v.name = 'FIXTURE_VENDOR_' || RIGHT('00' || CAST(seq.n AS VARCHAR), 2)
      AND v.deleted_at IS NULL
);

-- 3) Insert 300 fixture products spread across 10 vendors.
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
    (
        SELECT v.vendor_id
        FROM vendors v
        WHERE v.store_id = (
            SELECT COALESCE(
                (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
                (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
                (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
            )
        )
          AND v.name = 'FIXTURE_VENDOR_' || RIGHT('00' || CAST((MOD(seq.n - 1, 10) + 1) AS VARCHAR), 2)
          AND v.deleted_at IS NULL
        ORDER BY v.vendor_id
        LIMIT 1
    ) AS vendor_id,
    'NPLUS_PRODUCT_' || RIGHT('0000' || CAST(seq.n AS VARCHAR), 4) AS name,
    'NP-P-' || RIGHT('0000' || CAST(seq.n AS VARCHAR), 4) AS code,
    'EA',
    TRUE,
    3000 + MOD(seq.n, 700),
    6000 + MOD(seq.n, 900),
    4500 + MOD(seq.n, 800),
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT x AS n FROM SYSTEM_RANGE(1, 300)
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
      AND p.code = 'NP-P-' || RIGHT('0000' || CAST(seq.n AS VARCHAR), 4)
      AND p.deleted_at IS NULL
);

-- 4) Ensure inventory exists for each fixture product.
INSERT INTO inventories (
    product_id, display_stock, warehouse_stock, outgoing_reserved, incoming_reserved, reorder_trigger_point, deleted_at, created_at, updated_at
)
SELECT
    p.product_id,
    CAST(30 + MOD(p.product_id, 15) AS DECIMAL(10, 3)),
    CAST(200 + MOD(p.product_id, 50) AS DECIMAL(10, 3)),
    CAST(MOD(p.product_id, 7) AS DECIMAL(10, 3)),
    CAST(MOD(p.product_id, 11) AS DECIMAL(10, 3)),
    CAST(20 AS DECIMAL(10, 3)),
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM products p
WHERE p.code LIKE 'NP-P-%'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM inventories i
      WHERE i.product_id = p.product_id
  );

-- 5) Insert 200 fixture orders.
INSERT INTO orders (
    store_id, vendor_id, status, order_message, lead_time, expected_arrival,
    quote_received_at, deposit_confirmed_at, is_activate, total_price, deleted_at, created_at, updated_at
)
SELECT
    (
        SELECT COALESCE(
            (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
            (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
            (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
        )
    ) AS target_store_id,
    (
        SELECT v.vendor_id
        FROM vendors v
        WHERE v.store_id = (
            SELECT COALESCE(
                (SELECT u0.store_id FROM users u0 WHERE u0.username = 'fixture_admin' ORDER BY u0.user_id LIMIT 1),
                (SELECT u1.store_id FROM users u1 ORDER BY u1.user_id LIMIT 1),
                (SELECT s0.store_id FROM stores s0 ORDER BY s0.store_id LIMIT 1)
            )
        )
          AND v.name = 'FIXTURE_VENDOR_' || RIGHT('00' || CAST((MOD(seq.n - 1, 10) + 1) AS VARCHAR), 2)
          AND v.deleted_at IS NULL
        ORDER BY v.vendor_id
        LIMIT 1
    ) AS vendor_id,
    CASE MOD(seq.n, 4)
        WHEN 0 THEN 'REQUEST'
        WHEN 1 THEN 'IN_PRODUCTION'
        WHEN 2 THEN 'PENDING_SHIPMENT'
        ELSE 'DELIVERED'
    END,
    '[FIXTURE] N+1 order ' || RIGHT('000' || CAST(seq.n AS VARCHAR), 3),
    2 + MOD(seq.n, 5),
    DATEADD('DAY', 2 + MOD(seq.n, 5), CURRENT_DATE),
    DATEADD('DAY', -1 * MOD(seq.n, 3), CURRENT_DATE),
    DATEADD('DAY', -1 * MOD(seq.n, 2), CURRENT_DATE),
    TRUE,
    0,
    NULL,
    DATEADD('DAY', -1 * seq.n, CURRENT_TIMESTAMP),
    DATEADD('DAY', -1 * seq.n, CURRENT_TIMESTAMP)
FROM (
    SELECT x AS n FROM SYSTEM_RANGE(1, 200)
) seq
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.order_message = '[FIXTURE] N+1 order ' || RIGHT('000' || CAST(seq.n AS VARCHAR), 3)
      AND o.deleted_at IS NULL
);

-- 6) Insert 8 items per order.
INSERT INTO order_items (
    order_id, product_id, quantity, unit_price, amount, note, created_at, updated_at
)
SELECT
    o.order_id,
    p.product_id,
    1 + MOD(o_seq.n + item_seq.i, 5),
    p.cost_price,
    (1 + MOD(o_seq.n + item_seq.i, 5)) * p.cost_price,
    'fixture order item ' || CAST(item_seq.i AS VARCHAR),
    DATEADD('DAY', -1 * o_seq.n, CURRENT_TIMESTAMP),
    DATEADD('DAY', -1 * o_seq.n, CURRENT_TIMESTAMP)
FROM (
    SELECT x AS n FROM SYSTEM_RANGE(1, 200)
) o_seq
JOIN orders o
  ON o.order_message = '[FIXTURE] N+1 order ' || RIGHT('000' || CAST(o_seq.n AS VARCHAR), 3)
JOIN (
    SELECT x AS i FROM SYSTEM_RANGE(1, 8)
) item_seq
JOIN products p
  ON p.code = 'NP-P-' || RIGHT('0000' || CAST((((o_seq.n - 1) * 8 + item_seq.i - 1) % 300) + 1 AS VARCHAR), 4)
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items oi
    WHERE oi.order_id = o.order_id
);

-- 7) Update order total_price after item insert.
UPDATE orders o
SET total_price = (
    SELECT COALESCE(SUM(oi.amount), 0)
    FROM order_items oi
    WHERE oi.order_id = o.order_id
)
WHERE o.order_message LIKE '[FIXTURE] N+1 order %'
  AND o.deleted_at IS NULL;

-- 8) Insert receipts for first 150 orders.
INSERT INTO receipts (
    store_id, order_id, receipt_date, status, is_activate, deleted_at, created_at, updated_at
)
SELECT
    o.store_id,
    o.order_id,
    DATEADD('DAY', MOD(seq.n, 6), CURRENT_DATE),
    CASE MOD(seq.n, 3)
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'CONFIRMED'
        ELSE 'PENDING'
    END,
    TRUE,
    NULL,
    DATEADD('DAY', -1 * seq.n, CURRENT_TIMESTAMP),
    DATEADD('DAY', -1 * seq.n, CURRENT_TIMESTAMP)
FROM (
    SELECT x AS n FROM SYSTEM_RANGE(1, 150)
) seq
JOIN orders o
  ON o.order_message = '[FIXTURE] N+1 order ' || RIGHT('000' || CAST(seq.n AS VARCHAR), 3)
WHERE NOT EXISTS (
    SELECT 1
    FROM receipts r
    WHERE r.order_id = o.order_id
      AND r.deleted_at IS NULL
);

-- 9) Insert receipt items from order items.
INSERT INTO receipt_items (
    receipt_id, product_id, expected_quantity, actual_quantity, unit_price, amount, note, created_at, updated_at
)
SELECT
    r.receipt_id,
    oi.product_id,
    oi.quantity,
    CASE MOD(oi.order_item_id, 4)
        WHEN 0 THEN oi.quantity
        WHEN 1 THEN oi.quantity
        WHEN 2 THEN oi.quantity - 1
        ELSE NULL
    END AS actual_quantity,
    oi.unit_price,
    CASE MOD(oi.order_item_id, 4)
        WHEN 0 THEN oi.quantity * oi.unit_price
        WHEN 1 THEN oi.quantity * oi.unit_price
        WHEN 2 THEN (oi.quantity - 1) * oi.unit_price
        ELSE oi.quantity * oi.unit_price
    END AS amount,
    'fixture receipt item',
    r.created_at,
    r.updated_at
FROM receipts r
JOIN orders o ON o.order_id = r.order_id
JOIN order_items oi ON oi.order_id = o.order_id
WHERE o.order_message LIKE '[FIXTURE] N+1 order %'
  AND NOT EXISTS (
      SELECT 1
      FROM receipt_items ri
      WHERE ri.receipt_id = r.receipt_id
  );

-- 10) Quick checks
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
WHERE p.code LIKE 'NP-P-%'
  AND p.deleted_at IS NULL;

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
