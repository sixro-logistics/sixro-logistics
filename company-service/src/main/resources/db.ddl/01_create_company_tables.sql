-- 01_create_company_tables.sql
-- company_schema 내부에 p_company 테이블 생성
CREATE TABLE IF NOT EXISTS company_schema.p_company (
    company_id UUID PRIMARY KEY,
    hub_id UUID NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    company_type VARCHAR(20) NOT NULL,
    business_number VARCHAR(20) UNIQUE NOT NULL,
    zipcode VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_email VARCHAR(100),
    contact_phone VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 인덱스 생성 (조회 성능 최적화)
CREATE INDEX IF NOT EXISTS idx_company_hub_id ON company_schema.p_company(hub_id);
CREATE INDEX IF NOT EXISTS idx_company_name ON company_schema.p_company(company_name);
CREATE INDEX IF NOT EXISTS idx_company_type ON company_schema.p_company(company_type);
