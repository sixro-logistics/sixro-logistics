\set ON_ERROR_STOP on

-- 실행 DB: sixro_db
-- 실행 계정: ROLE/SCHEMA 생성 권한이 있는 관리자
-- 예시:
-- psql -h <host> -U <admin_user> -d sixro_db \
--   -v user_password='<password>' \
--   -f 00_create_user_role_and_schema.sql

\if :{?user_password}
\else
\echo 'ERROR: user_password 변수가 필요합니다.'
\quit
\endif

SELECT format(
               'CREATE ROLE user_user LOGIN PASSWORD %L',
               :'user_password'
       )
    WHERE NOT EXISTS (
    SELECT 1
    FROM pg_catalog.pg_roles
    WHERE rolname = 'user_user'
) \gexec

ALTER ROLE user_user
    WITH LOGIN
    PASSWORD :'user_password';

GRANT CONNECT ON DATABASE sixro_db TO user_user;

CREATE SCHEMA IF NOT EXISTS user_schema
    AUTHORIZATION user_user;

ALTER SCHEMA user_schema OWNER TO user_user;

-- 다른 서비스 계정이 user_schema 객체에 접근하지 못하도록
-- PostgreSQL의 PUBLIC 기본 권한을 제거합니다.
REVOKE ALL ON SCHEMA user_schema FROM PUBLIC;

GRANT USAGE, CREATE ON SCHEMA user_schema TO user_user;

ALTER ROLE user_user IN DATABASE sixro_db
    SET search_path = user_schema, public;
