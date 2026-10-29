-- =============================================================
-- 00. 샌드박스 데이터베이스 생성 — 관리자 계정으로 1회 실행
--
--   실행 주체 : root 등 CREATE/GRANT 권한이 있는 계정
--   실행 대상 : localhost MySQL 9.6.0
--
-- 왜 별도 파일인가
--   개발 계정 '2023314009'@'localhost' 의 전역 권한은 USAGE 한 줄이 전부다.
--       SELECT * FROM information_schema.user_privileges
--        WHERE grantee LIKE '%2023314009%';
--       → '2023314009'@'localhost' | def | USAGE | NO
--   전역 CREATE 권한이 없어 어떤 이름으로도 CREATE DATABASE 가 거부된다.
--       ERROR 1044 (42000): Access denied ... to database 'webapplication_sandbox'
--   따라서 DB 생성과 권한 부여는 관리자가 한 번 해주어야 하고,
--   그 뒤로는 개발 계정이 테이블 생성부터 전부 수행할 수 있다.
--
-- 실행 후
--   sql/schema.sql → sql/data.sql 순으로 개발 계정이 적용한다.
-- =============================================================

CREATE DATABASE IF NOT EXISTS webapplication_sandbox
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON webapplication_sandbox.*
    TO '2023314009'@'localhost';

FLUSH PRIVILEGES;

-- 확인용 --------------------------------------------------------
-- SHOW GRANTS FOR '2023314009'@'localhost';
--   기대 결과에 아래 한 줄이 추가되어 있어야 한다.
--   GRANT ALL PRIVILEGES ON `webapplication_sandbox`.* TO `2023314009`@`localhost`
