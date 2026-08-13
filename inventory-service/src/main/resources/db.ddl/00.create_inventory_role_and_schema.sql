\set ON_ERROR_STOP on

-- 배포 환경에서 PostgreSQL 컨테이너에 INVENTORY_DB_PASSWORD를 전달해야 합니다.
\getenv inventory_db_password INVENTORY_DB_PASSWORD

\if :{?inventory_db_password}
\else
\echo 'INVENTORY_DB_PASSWORD environment variable is required.'
\quit
\endif

-- Inventory Service 전용 DB 계정 생성
SELECT format(
               'CREATE ROLE inventory_user LOGIN PASSWORD %L',
               :'inventory_db_password'
       )
    WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = 'inventory_user'
) \gexec

-- 재실행 시에도 배포 환경의 비밀번호를 반영
ALTER ROLE inventory_user WITH LOGIN PASSWORD :'inventory_db_password';

-- 물리 DB 접속 및 전용 스키마 권한 설정
GRANT CONNECT ON DATABASE sixro_db TO inventory_user;

CREATE SCHEMA IF NOT EXISTS inventory_schema AUTHORIZATION inventory_user;
ALTER SCHEMA inventory_schema OWNER TO inventory_user;

GRANT USAGE, CREATE ON SCHEMA inventory_schema TO inventory_user;
ALTER ROLE inventory_user SET search_path TO inventory_schema, public;