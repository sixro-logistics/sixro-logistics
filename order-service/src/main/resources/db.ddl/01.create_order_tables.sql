\set ON_ERROR_STOP on

SET ROLE order_user;

-- 주문
CREATE TABLE IF NOT EXISTS order_schema.p_order
(
    order_id             UUID         NOT NULL,
    receiver_id          UUID         NOT NULL,
    hub_id               UUID         NOT NULL,
    receiver_company_id  UUID         NOT NULL,
    delivery_address     VARCHAR(255) NOT NULL,
    delivery_deadline    TIMESTAMP    NOT NULL,
    requests             VARCHAR(255),
    order_status         VARCHAR(255) NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           UUID         NOT NULL,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           UUID         NOT NULL,
    deleted_at           TIMESTAMP,
    deleted_by           UUID,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_order PRIMARY KEY (order_id),

    CONSTRAINT ck_order_status
    CHECK (
              order_status IN (
              'CREATED',
              'DELIVERY_CREATED',
              'FAILED',
              'CANCELED'
                              )
    )
    );

-- 주문 상품
CREATE TABLE IF NOT EXISTS order_schema.p_order_item
(
    order_item_id  UUID         NOT NULL,
    order_id       UUID         NOT NULL,
    product_id     UUID         NOT NULL,
    product_name   VARCHAR(100) NOT NULL,
    product_price  INTEGER      NOT NULL,
    quantity       INTEGER      NOT NULL,
    company_id     UUID         NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     UUID         NOT NULL,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     UUID         NOT NULL,
    deleted_at     TIMESTAMP,
    deleted_by     UUID,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_order_item
    PRIMARY KEY (order_item_id),

    CONSTRAINT fk_order_item_order
    FOREIGN KEY (order_id)
    REFERENCES order_schema.p_order (order_id),

    CONSTRAINT ck_order_item_quantity
    CHECK (quantity >= 1)
    );

-- 주문 멱등성
CREATE TABLE IF NOT EXISTS order_schema.p_order_idempotency
(
    id                UUID NOT NULL,
    idempotency_key   UUID NOT NULL,
    order_id          UUID NOT NULL,

    CONSTRAINT pk_order_idempotency
    PRIMARY KEY (id),

    CONSTRAINT uk_order_idempotency_key
    UNIQUE (idempotency_key),

    CONSTRAINT fk_order_idempotency_order
    FOREIGN KEY (order_id)
    REFERENCES order_schema.p_order (order_id)
    );

-- 처리된 이벤트
CREATE TABLE IF NOT EXISTS order_schema.p_order_processed_event
(
    id       UUID NOT NULL,
    event_id UUID NOT NULL,

    CONSTRAINT pk_order_processed_event
    PRIMARY KEY (id),

    CONSTRAINT uk_order_processed_event_event_id
    UNIQUE (event_id)
    );

-- 주문 Outbox
CREATE TABLE IF NOT EXISTS order_schema.p_order_outbox
(
    outbox_id      UUID         NOT NULL,
    aggregate_id   UUID         NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(255) NOT NULL,

    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     UUID         NOT NULL,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     UUID         NOT NULL,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP,
    deleted_by     UUID,

    CONSTRAINT pk_order_outbox
    PRIMARY KEY (outbox_id),

    CONSTRAINT ck_order_outbox_aggregate_type
    CHECK (
              aggregate_type IN ('ORDER')
    ),

    CONSTRAINT ck_order_outbox_event_type
    CHECK (
              event_type IN (
              'ORDER_CREATED',
              'ORDER_FAILED',
              'ORDER_CANCELED'
                            )
    ),

    CONSTRAINT ck_order_outbox_status
    CHECK (
              status IN (
              'PENDING',
              'PUBLISHED',
              'FAILED'
                        )
    )
    );