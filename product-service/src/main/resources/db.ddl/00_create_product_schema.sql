-- 00_create_schema.sql
-- product-service용 스키마 생성
CREATE SCHEMA IF NOT EXISTS product_schema;

-- UUID 확장 기능 활성화 (필요시)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";