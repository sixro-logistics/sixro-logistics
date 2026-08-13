-- 02_create_ai_log_tables.sql
-- notification_schema 내부에 p_ai_log 테이블 생성 (notification-service에 포함된다고 가정)

CREATE TABLE IF NOT EXISTS notification_schema.p_ai_log (
    ai_request_log_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    request_data JSON NOT NULL,
    response_data JSON,
    request_status VARCHAR(20),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 인덱스 생성 (조회 성능 최적화)
CREATE INDEX IF NOT EXISTS idx_ai_log_order_id ON notification_schema.p_ai_log(order_id);
CREATE INDEX IF NOT EXISTS idx_ai_log_status ON notification_schema.p_ai_log(request_status);
