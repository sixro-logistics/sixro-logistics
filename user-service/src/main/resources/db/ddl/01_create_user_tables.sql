\set ON_ERROR_STOP on

-- 실행 DB: sixro_db
-- 실행 계정: user_user
-- 선행 파일: 00_create_user_role_and_schema.sql

BEGIN;

CREATE TABLE IF NOT EXISTS user_schema.p_user
(
    user_id UUID NOT NULL,
    username VARCHAR(10) NOT NULL,
    password VARCHAR(255) NOT NULL,
    slack_id VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    affiliation_id UUID,
    affiliation_type VARCHAR(30),
    user_status VARCHAR(30) NOT NULL,
    reviewed_at TIMESTAMP(6),
    reviewed_by UUID,
    rejected_reason VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    updated_by UUID NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    deleted_by UUID,

    CONSTRAINT pk_p_user PRIMARY KEY (user_id),

    -- 삭제된 계정도 username과 Slack ID를 재사용하지 않습니다.
    CONSTRAINT uk_p_user_username UNIQUE (username),
    CONSTRAINT uk_p_user_slack_id UNIQUE (slack_id),

    CONSTRAINT ck_p_user_username_format
    CHECK (username ~ '^[a-z0-9]{4,10}$'),

    CONSTRAINT ck_p_user_password_not_blank
    CHECK (BTRIM(password) <> ''),

    CONSTRAINT ck_p_user_slack_id_not_blank
    CHECK (BTRIM(slack_id) <> ''),

    CONSTRAINT ck_p_user_role
    CHECK (
              role IN (
              'MASTER_ADMIN',
              'HUB_ADMIN',
              'DELIVERY_MANAGER',
              'COMPANY_MANAGER'
                      )
    ),

    CONSTRAINT ck_p_user_status
    CHECK (user_status IN ('PENDING', 'APPROVED', 'REJECTED')),

    CONSTRAINT ck_p_user_affiliation_type
    CHECK (
              affiliation_type IS NULL
              OR affiliation_type IN ('HUB', 'COMPANY')
    ),

    CONSTRAINT ck_p_user_role_affiliation
    CHECK (
(
              role = 'MASTER_ADMIN'
              AND affiliation_id IS NULL
              AND affiliation_type IS NULL
)
    OR
(
    role IN ('HUB_ADMIN', 'DELIVERY_MANAGER')
    AND affiliation_id IS NOT NULL
    AND affiliation_type = 'HUB'
    )
    OR
(
    role = 'COMPANY_MANAGER'
    AND affiliation_id IS NOT NULL
    AND affiliation_type = 'COMPANY'
)
    ),

    CONSTRAINT ck_p_user_review_state
    CHECK (
(
              user_status = 'PENDING'
              AND reviewed_at IS NULL
              AND reviewed_by IS NULL
              AND rejected_reason IS NULL
)
    OR
(
    user_status = 'APPROVED'
    AND reviewed_at IS NOT NULL
    AND reviewed_by IS NOT NULL
    AND rejected_reason IS NULL
)
    OR
(
    user_status = 'REJECTED'
    AND reviewed_at IS NOT NULL
    AND reviewed_by IS NOT NULL
    AND rejected_reason IS NOT NULL
    AND BTRIM(rejected_reason) <> ''
    )
    ),

    CONSTRAINT ck_p_user_soft_delete
    CHECK (
(
              is_deleted = FALSE
              AND deleted_at IS NULL
              AND deleted_by IS NULL
)
    OR
(
    is_deleted = TRUE
    AND deleted_at IS NOT NULL
    AND deleted_by IS NOT NULL
)
    )
    );

COMMENT ON TABLE user_schema.p_user
    IS '사용자 계정, 역할, 소속 및 가입 심사 정보';

COMMENT ON COLUMN user_schema.p_user.password
    IS 'Auth Service에서 BCrypt로 암호화한 비밀번호';

CREATE INDEX IF NOT EXISTS idx_p_user_active_created_at
    ON user_schema.p_user (created_at DESC)
    WHERE is_deleted = FALSE
    AND deleted_at IS NULL
    AND deleted_by IS NULL;

CREATE INDEX IF NOT EXISTS idx_p_user_active_role_status
    ON user_schema.p_user (role, user_status)
    WHERE is_deleted = FALSE
    AND deleted_at IS NULL
    AND deleted_by IS NULL;

CREATE INDEX IF NOT EXISTS idx_p_user_active_affiliation
    ON user_schema.p_user (affiliation_type, affiliation_id)
    WHERE is_deleted = FALSE
    AND deleted_at IS NULL
    AND deleted_by IS NULL;

CREATE TABLE IF NOT EXISTS user_schema.p_user_outbox_events
(
    event_id UUID NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6),
    retry_count INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT pk_p_user_outbox_events PRIMARY KEY (event_id),

    CONSTRAINT ck_p_user_outbox_aggregate_type
    CHECK (aggregate_type = 'USER'),

    CONSTRAINT ck_p_user_outbox_event_type
    CHECK (
              event_type IN (
              'USER_CREATED',
              'USER_APPROVED',
              'USER_REJECTED',
              'USER_DEACTIVATED',
              'USER_ROLE_CHANGED',
              'USER_AFFILIATION_CHANGED'
                            )
    ),

    CONSTRAINT ck_p_user_outbox_status
    CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),

    CONSTRAINT ck_p_user_outbox_retry_count
    CHECK (retry_count BETWEEN 0 AND 3),

    CONSTRAINT ck_p_user_outbox_publish_state
    CHECK (
(
              status = 'PUBLISHED'
              AND published_at IS NOT NULL
)
    OR
(
    status IN ('PENDING', 'FAILED')
    AND published_at IS NULL
    )
    ),

    CONSTRAINT ck_p_user_outbox_payload_json
    CHECK (jsonb_typeof(payload::jsonb) = 'object')
    );

COMMENT ON TABLE user_schema.p_user_outbox_events
    IS '사용자 도메인 Event 발행을 위한 Transactional Outbox';

COMMENT ON COLUMN user_schema.p_user_outbox_events.event_id
    IS 'Kafka event-id Header로 전달되는 Event 식별자';

CREATE INDEX IF NOT EXISTS idx_p_user_outbox_pending_created_at
    ON user_schema.p_user_outbox_events (created_at ASC)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_p_user_outbox_aggregate_created_at
    ON user_schema.p_user_outbox_events (aggregate_id, created_at DESC);

ALTER TABLE user_schema.p_user OWNER TO user_user;
ALTER TABLE user_schema.p_user_outbox_events OWNER TO user_user;

GRANT SELECT, INSERT, UPDATE, DELETE
      ON ALL TABLES IN SCHEMA user_schema
          TO user_user;

ALTER DEFAULT PRIVILEGES FOR ROLE user_user IN SCHEMA user_schema
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO user_user;

COMMIT;

