\set ON_ERROR_STOP on

-- 배포 환경에서 PostgreSQL 컨테이너에 ORDER_DB_PASSWORD를 전달해야 합니다.
\getenv order_db_password ORDER_DB_PASSWORD

\if :{?order_db_password}
\else
\echo 'ORDER_DB_PASSWORD environment variable is required.'
\quit
\endif

-- Order Service 전용 DB 계정 생성
SELECT format(
               'CREATE ROLE order_user LOGIN PASSWORD %L',
               :'order_db_password'
       )
    WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = 'order_user'
) \gexec

-- 재실행 시에도 배포 환경의 비밀번호를 반영
ALTER ROLE order_user WITH LOGIN PASSWORD :'order_db_password';

-- 물리 DB 접속 및 전용 스키마 권한 설정
GRANT CONNECT ON DATABASE sixro_db TO order_user;

CREATE SCHEMA IF NOT EXISTS order_schema AUTHORIZATION order_user;
ALTER SCHEMA order_schema OWNER TO order_user;

GRANT USAGE, CREATE ON SCHEMA order_schema TO order_user;
ALTER ROLE order_user SET search_path TO order_schema, public;