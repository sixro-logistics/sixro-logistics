-- 00_create_user_role_and_schema.sql
-- company-service용 스키마 생성
CREATE SCHEMA IF NOT EXISTS company_schema;

-- 필요시 공통 권한이나 익스텐션 설정 (예: UUID 생성용)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";