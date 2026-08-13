\set ON_ERROR_STOP on

SET ROLE inventory_user;

-- 재고
CREATE TABLE IF NOT EXISTS inventory_schema.p_inventory
(
    inventory_id UUID         NOT NULL,
    hub_id       UUID         NOT NULL,
    company_id   UUID         NOT NULL,
    product_id   UUID         NOT NULL,
    stock        INTEGER      NOT NULL,

    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   UUID         NOT NULL,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   UUID         NOT NULL,
    deleted_at   TIMESTAMP,
    deleted_by   UUID,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_inventory
    PRIMARY KEY (inventory_id),

    CONSTRAINT uk_inventory_hub_product
    UNIQUE (hub_id, product_id),

    CONSTRAINT ck_inventory_stock
    CHECK (stock >= 0)
    );

-- 재고 멱등성
CREATE TABLE IF NOT EXISTS inventory_schema.p_inventory_idempotency
(
    id                UUID         NOT NULL,
    idempotency_key   UUID         NOT NULL,
    operation         VARCHAR(20)  NOT NULL,

    CONSTRAINT pk_inventory_idempotency
    PRIMARY KEY (id),

    CONSTRAINT uk_inventory_idempotency_key
    UNIQUE (idempotency_key),

    CONSTRAINT ck_inventory_idempotency_operation
    CHECK (
              operation IN (
              'DEDUCT',
              'RESTORE'
                           )
    )
    );

-- 처리된 이벤트
CREATE TABLE IF NOT EXISTS inventory_schema.p_inventory_processed_event
(
    id       UUID NOT NULL,
    event_id UUID NOT NULL,

    CONSTRAINT pk_inventory_processed_event
    PRIMARY KEY (id),

    CONSTRAINT uk_processed_event_event_id
    UNIQUE (event_id)
    );