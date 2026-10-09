ALTER TABLE IF EXISTS ticket_types
    ADD COLUMN IF NOT EXISTS category VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    ADD COLUMN IF NOT EXISTS sold_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS orders
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    ADD COLUMN IF NOT EXISTS idempotency_key UUID;

DO $$
DECLARE
    inconsistent_rows INTEGER;
BEGIN
    IF to_regclass(current_schema() || '.ticket_types') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM ticket_types
            WHERE lower(name) NOT LIKE '%vip%'
              AND lower(name) NOT LIKE '%normal%'
              AND lower(name) NOT LIKE '%general%'
        ) THEN
            RAISE EXCEPTION 'Unclassified ticket type names found; map them to NORMAL or VIP before migrating';
        END IF;

        UPDATE ticket_types
        SET category = CASE
            WHEN lower(name) LIKE '%vip%' THEN 'VIP'
            ELSE 'NORMAL'
        END,
        currency = upper(btrim(coalesce(nullif(currency, ''), 'EUR')));

        IF to_regclass(current_schema() || '.orders') IS NOT NULL THEN
            UPDATE orders
            SET currency = upper(btrim(coalesce(nullif(currency, ''), 'EUR')));
        END IF;

        IF to_regclass(current_schema() || '.orders') IS NOT NULL
                AND to_regclass(current_schema() || '.order_items') IS NOT NULL THEN
            WITH order_quantities AS (
                SELECT oi.ticket_type_id,
                       coalesce(sum(oi.quantity) FILTER (WHERE o.status = 'PENDING'), 0)
                           AS pending_quantity,
                       coalesce(sum(oi.quantity) FILTER (WHERE o.status IN ('PAID', 'DEMO')), 0)
                           AS sold_quantity
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                GROUP BY oi.ticket_type_id
            )
            SELECT count(*) INTO inconsistent_rows
            FROM ticket_types t
            LEFT JOIN order_quantities q ON q.ticket_type_id = t.id
            WHERE t.total_quantity <> t.available_quantity
                    + coalesce(q.pending_quantity, 0)
                    + coalesce(q.sold_quantity, 0);

            IF inconsistent_rows > 0 THEN
                RAISE EXCEPTION 'Inventory counters do not match order history for % ticket types; reconcile before migrating',
                    inconsistent_rows;
            END IF;

            WITH order_quantities AS (
                SELECT oi.ticket_type_id,
                       coalesce(sum(oi.quantity) FILTER (WHERE o.status = 'PENDING'), 0)
                           AS pending_quantity,
                       coalesce(sum(oi.quantity) FILTER (WHERE o.status IN ('PAID', 'DEMO')), 0)
                           AS sold_quantity
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                GROUP BY oi.ticket_type_id
            )
            UPDATE ticket_types t
            SET reserved_quantity = coalesce(q.pending_quantity, 0),
                sold_quantity = coalesce(q.sold_quantity, 0)
            FROM order_quantities q
            WHERE t.id = q.ticket_type_id;
        ELSE
            UPDATE ticket_types
            SET reserved_quantity = 0,
                sold_quantity = greatest(total_quantity - available_quantity, 0);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'ck_ticket_types_inventory'
              AND conrelid = to_regclass(current_schema() || '.ticket_types')
        ) THEN
            ALTER TABLE ticket_types
                ADD CONSTRAINT ck_ticket_types_inventory CHECK (
                    total_quantity >= 0
                    AND available_quantity >= 0
                    AND sold_quantity >= 0
                    AND reserved_quantity >= 0
                    AND total_quantity = available_quantity + sold_quantity + reserved_quantity
                );
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'ck_ticket_types_category'
              AND conrelid = to_regclass(current_schema() || '.ticket_types')
        ) THEN
            ALTER TABLE ticket_types
                ADD CONSTRAINT ck_ticket_types_category CHECK (category IN ('NORMAL', 'VIP'));
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'ck_ticket_types_currency'
              AND conrelid = to_regclass(current_schema() || '.ticket_types')
        ) THEN
            ALTER TABLE ticket_types
                ADD CONSTRAINT ck_ticket_types_currency CHECK (currency ~ '^[A-Z]{3}$');
        END IF;
    END IF;

    IF to_regclass(current_schema() || '.orders') IS NOT NULL
            AND NOT EXISTS (
                SELECT 1 FROM pg_constraint
                WHERE conname = 'ck_orders_currency'
                  AND conrelid = to_regclass(current_schema() || '.orders')
            ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT ck_orders_currency CHECK (currency ~ '^[A-Z]{3}$');
    END IF;

    IF to_regclass(current_schema() || '.orders') IS NOT NULL
            AND NOT EXISTS (
                SELECT 1 FROM pg_constraint
                WHERE conname = 'uk_orders_user_idempotency_key'
                  AND conrelid = to_regclass(current_schema() || '.orders')
            ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT uk_orders_user_idempotency_key UNIQUE (user_id, idempotency_key);
    END IF;
END $$;
