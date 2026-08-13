\set ON_ERROR_STOP on

SET ROLE delivery_user;

-- 배송 담당자
CREATE TABLE IF NOT EXISTS delivery_schema.p_delivery_manager
(
    delivery_manager_id UUID         NOT NULL,
    hub_id              UUID,
    manager_type        VARCHAR(255) NOT NULL,
    manager_status      VARCHAR(255) NOT NULL DEFAULT 'AVAILABLE',
    delivery_sequence   INTEGER      NOT NULL,
    created_at          TIMESTAMP    NOT NULL,
    created_by          UUID         NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    updated_by          UUID         NOT NULL,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,
    deleted_by          UUID,

    CONSTRAINT pk_delivery_manager PRIMARY KEY (delivery_manager_id),
    CONSTRAINT ck_delivery_manager_type
        CHECK (manager_type IN ('HUB_DELIVERY', 'COMPANY_DELIVERY')),
    CONSTRAINT ck_delivery_manager_status
        CHECK (manager_status IN ('AVAILABLE', 'IN_DELIVERY', 'OFF_DUTY')),
    CONSTRAINT ck_delivery_manager_sequence
        CHECK (delivery_sequence > 0),
    CONSTRAINT ck_delivery_manager_type_hub
        CHECK (
            (manager_type = 'HUB_DELIVERY' AND hub_id IS NULL)
            OR (manager_type = 'COMPANY_DELIVERY' AND hub_id IS NOT NULL)
        )
);

-- 배송
CREATE TABLE IF NOT EXISTS delivery_schema.p_delivery
(
    delivery_id          UUID         NOT NULL,
    order_id             UUID         NOT NULL,
    recipient_company_id UUID         NOT NULL,
    origin_hub_id        UUID         NOT NULL,
    dest_hub_id          UUID         NOT NULL,
    delivery_manager_id  UUID,
    delivery_status      VARCHAR(255) NOT NULL DEFAULT 'HUB_WAITING',
    delivery_address     VARCHAR(500) NOT NULL,
    delivery_deadline    TIMESTAMP,
    requests             VARCHAR(255),
    recipient_name       VARCHAR(50)  NOT NULL,
    recipient_slack_id   VARCHAR(100) NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    created_by           UUID         NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    updated_by           UUID         NOT NULL,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMP,
    deleted_by           UUID,

    CONSTRAINT pk_delivery PRIMARY KEY (delivery_id),
    CONSTRAINT uk_delivery_order UNIQUE (order_id),
    CONSTRAINT fk_delivery_manager
        FOREIGN KEY (delivery_manager_id)
            REFERENCES delivery_schema.p_delivery_manager (delivery_manager_id),
    CONSTRAINT ck_delivery_status
        CHECK (delivery_status IN (
            'HUB_WAITING',
            'HUB_IN_TRANSIT',
            'DESTINATION_HUB_ARRIVED',
            'COMPANY_DELIVERY_IN_PROGRESS',
            'DELIVERED',
            'CANCELLED',
            'FAILED'
        ))
);

-- 배송별 공급 업체 ID
CREATE TABLE IF NOT EXISTS delivery_schema.p_delivery_supplier_company
(
    delivery_id        UUID NOT NULL,
    supplier_company_id UUID NOT NULL,

    CONSTRAINT uk_delivery_supplier_company
        UNIQUE (delivery_id, supplier_company_id),
    CONSTRAINT fk_delivery_supplier_company_delivery
        FOREIGN KEY (delivery_id)
            REFERENCES delivery_schema.p_delivery (delivery_id)
            ON DELETE CASCADE
);

-- 허브 간 배송경로
CREATE TABLE IF NOT EXISTS delivery_schema.p_delivery_route
(
    delivery_route_id  UUID         NOT NULL,
    delivery_id        UUID         NOT NULL,
    route_sequence     INTEGER      NOT NULL,
    origin_hub_id      UUID         NOT NULL,
    dest_hub_id        UUID         NOT NULL,
    expected_distance_m BIGINT       NOT NULL,
    expected_duration_s BIGINT       NOT NULL,
    route_status       VARCHAR(255) NOT NULL DEFAULT 'HUB_TRANSIT_WAITING',
    delivery_manager_id UUID,
    started_at         TIMESTAMP,
    completed_at       TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL,
    created_by         UUID         NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    updated_by         UUID         NOT NULL,
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMP,
    deleted_by         UUID,

    CONSTRAINT pk_delivery_route PRIMARY KEY (delivery_route_id),
    CONSTRAINT uk_delivery_route_sequence UNIQUE (delivery_id, route_sequence),
    CONSTRAINT fk_delivery_route_delivery
        FOREIGN KEY (delivery_id)
            REFERENCES delivery_schema.p_delivery (delivery_id),
    CONSTRAINT fk_delivery_route_manager
        FOREIGN KEY (delivery_manager_id)
            REFERENCES delivery_schema.p_delivery_manager (delivery_manager_id),
    CONSTRAINT ck_delivery_route_sequence CHECK (route_sequence > 0),
    CONSTRAINT ck_delivery_route_distance CHECK (expected_distance_m >= 0),
    CONSTRAINT ck_delivery_route_duration CHECK (expected_duration_s >= 0),
    CONSTRAINT ck_delivery_route_status
        CHECK (route_status IN (
            'HUB_TRANSIT_WAITING',
            'HUB_IN_TRANSIT',
            'HUB_ARRIVED',
            'CANCELLED',
            'FAILED'
        ))
);

-- Kafka 발행 대상 Outbox
CREATE TABLE IF NOT EXISTS delivery_schema.p_outbox
(
    event_id      UUID         NOT NULL,
    aggregate_id  UUID         NOT NULL,
    event_type    VARCHAR(255) NOT NULL,
    trace_id      VARCHAR(255) NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP    NOT NULL,
    published_at  TIMESTAMP,

    CONSTRAINT pk_delivery_outbox PRIMARY KEY (event_id),
    CONSTRAINT ck_delivery_outbox_event_type
        CHECK (event_type IN ('DELIVERY_CREATED', 'DELIVERY_CREATION_FAILED')),
    CONSTRAINT ck_delivery_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED'))
);

-- 배송 검색
CREATE INDEX IF NOT EXISTS idx_delivery_search
    ON delivery_schema.p_delivery (is_deleted, delivery_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_delivery_origin_hub
    ON delivery_schema.p_delivery (origin_hub_id);
CREATE INDEX IF NOT EXISTS idx_delivery_dest_hub
    ON delivery_schema.p_delivery (dest_hub_id);
CREATE INDEX IF NOT EXISTS idx_delivery_manager
    ON delivery_schema.p_delivery (delivery_manager_id);
CREATE INDEX IF NOT EXISTS idx_delivery_deadline
    ON delivery_schema.p_delivery (delivery_deadline);

-- 배송경로 검색 및 순차 조회
CREATE INDEX IF NOT EXISTS idx_delivery_route_search
    ON delivery_schema.p_delivery_route (is_deleted, route_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_delivery_route_origin_hub
    ON delivery_schema.p_delivery_route (origin_hub_id);
CREATE INDEX IF NOT EXISTS idx_delivery_route_dest_hub
    ON delivery_schema.p_delivery_route (dest_hub_id);
CREATE INDEX IF NOT EXISTS idx_delivery_route_manager
    ON delivery_schema.p_delivery_route (delivery_manager_id);

-- 담당자 조회 및 순번 배정
CREATE INDEX IF NOT EXISTS idx_delivery_manager_assignment
    ON delivery_schema.p_delivery_manager
        (is_deleted, manager_type, hub_id, manager_status, delivery_sequence);

-- 발행 대기 Outbox Polling
CREATE INDEX IF NOT EXISTS idx_outbox_status_created_at
    ON delivery_schema.p_outbox (status, created_at);

RESET ROLE;
