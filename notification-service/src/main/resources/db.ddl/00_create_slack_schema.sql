-- 00_create_schema.sql
-- notification-service용 스키마 생성
CREATE SCHEMA IF NOT EXISTS notification_schema;

-- UUID 확장 기능 활성화 (필요시)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ENUM 타입 생성 (send_status 용)
DO $$ BEGIN
    CREATE TYPE notification_schema.send_status_enum AS ENUM ('PENDING', 'SENT', 'FAILED', 'RETRYING');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;
