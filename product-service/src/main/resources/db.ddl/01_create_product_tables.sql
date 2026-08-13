-- 01_create_product_tables.sql
-- product_schema 내부에 p_product 테이블 생성

CREATE TABLE IF NOT EXISTS product_schema.p_product (
    product_id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    hub_id UUID NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 인덱스 생성 (조회 성능 최적화)
CREATE INDEX IF NOT EXISTS idx_product_company_id ON product_schema.p_product(company_id);
CREATE INDEX IF NOT EXISTS idx_product_hub_id ON product_schema.p_product(hub_id);
CREATE INDEX IF NOT EXISTS idx_product_name ON product_schema.p_product(product_name);
