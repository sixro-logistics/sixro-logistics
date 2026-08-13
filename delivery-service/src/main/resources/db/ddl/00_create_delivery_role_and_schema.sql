\set ON_ERROR_STOP on

-- 배포 환경에서 PostgreSQL 컨테이너에 DELIVERY_DB_PASSWORD를 전달해야 합니다.
\getenv delivery_db_password DELIVERY_DB_PASSWORD

\if :{?delivery_db_password}
\else
    \echo 'DELIVERY_DB_PASSWORD environment variable is required.'
    \quit
\endif

-- Delivery Service 전용 DB 계정 생성
SELECT format(
               'CREATE ROLE delivery_user LOGIN PASSWORD %L',
               :'delivery_db_password'
       )
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = 'delivery_user'
) \gexec

-- 재실행 시에도 배포 환경의 비밀번호를 반영
ALTER ROLE delivery_user WITH LOGIN PASSWORD :'delivery_db_password';

-- 물리 DB 접속 및 전용 스키마 권한 설정
GRANT CONNECT ON DATABASE sixro_db TO delivery_user;

CREATE SCHEMA IF NOT EXISTS delivery_schema AUTHORIZATION delivery_user;
ALTER SCHEMA delivery_schema OWNER TO delivery_user;

GRANT USAGE, CREATE ON SCHEMA delivery_schema TO delivery_user;
ALTER ROLE delivery_user SET search_path TO delivery_schema, public;
