-- 01_create_slack_tables.sql
-- notification_schema 내부에 p_slack_message 테이블 생성

CREATE TABLE IF NOT EXISTS notification_schema.p_slack_message (
    slack_message_id UUID PRIMARY KEY,
    sender_type VARCHAR(20) NOT NULL,
    sender_id UUID NOT NULL,
    sender_slack_id VARCHAR(255) NOT NULL,
    receiver_slack_id VARCHAR(255) NOT NULL,
    message_content TEXT NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    send_status notification_schema.send_status_enum NOT NULL,
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
CREATE INDEX IF NOT EXISTS idx_slack_message_sender_id ON notification_schema.p_slack_message(sender_id);
CREATE INDEX IF NOT EXISTS idx_slack_message_status ON notification_schema.p_slack_message(send_status);
